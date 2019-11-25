package azpubsub.contextvalidator.kafka.security.auth

import java.net.InetAddress
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.{Date, Locale, TimeZone}

import azpubsub.kafka.security.auth.TokenValidator
import azpubsub.kafka.security.authenticator.AzPubSubOAuthBearerToken
import kafka.common.{NotificationHandler, ZkNodeChangeNotificationListener}
import kafka.metrics.KafkaMetricsGroup
import kafka.network.RequestChannel.Session
import kafka.security.auth.{Acl, All, Allow, Authorizer, DelegationToken, Describe, Group, Operation, PermissionType, Resource, ResourceType, Topic, TransactionalId}
import azpubsub.contextvalidator.kafka.security.auth.AzPubSubAclAuthorizer.VersionedAcls
import kafka.server.KafkaConfig
import kafka.utils.CoreUtils.{inReadLock, inWriteLock}
import kafka.utils.{CoreUtils, Json}
import kafka.zk.{KafkaZkClient, LiteralAclChangeStore, ZkAclChangeStore}
import org.apache.kafka.common.security.auth.KafkaPrincipal
import org.apache.kafka.common.utils.Time
import org.apache.kafka.common.resource.PatternType

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.{Random, Try}

object  AzPubSubAclAuthorizer{
  val AzPubSubAclAuthorizingRequest = "AzPubSubAclAuthorizingRequest"
  val TokenDesearializationFailRateMs = "TokenDesearializationFailRateMs"
  val Saml2TokenTimeFormatter = "M/dd/yyyy hh:mm:ss a"
  val TokenInvalidFromDatetimeRateMs = "TokenInvalidFromDatetimeRateMs"
  val TokenExpiredRateMs = "TokenExpiredRateMs"
  val TopicAuthorizationUsingTokenSuccessfulRateMs = "TopicAuthorizationUsingTokenSuccessfulRateMs"
  val TokenNotAuthorizedForTopicRateMs = "TokenNotAuthorizedForTopicRateMs"
  val TokenValidatorClassPathKey = "token.validator.class"

  val ZkUrlProp = "authorizer.zookeeper.url"
  val ZkConnectionTimeOutProp = "authorizer.zookeeper.connection.timeout.ms"
  val ZkSessionTimeOutProp = "authorizer.zookeeper.session.timeout.ms"
  val ZkMaxInFlightRequests = "authorizer.zookeeper.max.in.flight.requests"
  val ValidateTokenInMinutes = "validate.token.in.minutes"
  val UnixEpochTimeZeroTicks = 621355968000000000L

  case class VersionedAcls(acls: Set[Acl], zkVersion: Int)
}


class AzPubSubAclAuthorizer extends Authorizer with KafkaMetricsGroup {

  private var brokerHosts  = Set[String]()
  private val cacheTokenLastValidatedTime: mutable.HashMap[String, Date]  = new mutable.HashMap();
  private var tokenAuthenticator : TokenValidator = null
  private var periodToValidateTokenInMinutes : Int = 60
  private var zkClient: KafkaZkClient = null
  private var aclChangeListener: ZkNodeChangeNotificationListener = null
  private val aclCache = new scala.collection.mutable.HashMap[Resource, AzPubSubAclAuthorizer.VersionedAcls]
  private val lock = new ReentrantReadWriteLock()
  private val retryBackoffMs = 100
  private val retryBackoffJitterMs = 50
  private var topicsWhiteListed: mutable.HashSet[String] = new mutable.HashSet[String] {}

  protected[auth] var maxUpdateRetries = 10

  override def configure(javaConfigs: java.util.Map[String, _]): Unit = {
    val configs = javaConfigs.asScala
    val props = new java.util.Properties()
    configs.foreach { case (key, value) => props.put(key, value.toString) }

    tokenAuthenticator = CoreUtils.createObject[TokenValidator](configs.get(KafkaConfig.AzpubsubTokenValidatorClassProp).get.toString)

    if(null != tokenAuthenticator) {
      tokenAuthenticator.configure(javaConfigs)
    }


    if(props.containsKey(KafkaConfig.AzPubSubTopicWhiteListProp)) {
      topicsWhiteListed = new mutable.HashSet[String] {}
      val tmpTopics = props.get(KafkaConfig.AzPubSubTopicWhiteListProp).toString.split(",")
      tmpTopics.foreach(t => topicsWhiteListed.add(t))
    }

    val kafkaConfig = KafkaConfig.fromProps(props, doLog = false)
    periodToValidateTokenInMinutes = if (props.containsKey(KafkaConfig.AzPubSubTopicWhiteListProp)) props.get(KafkaConfig.AzpubsubValidateTokenInMinutesProp).toString.toInt else kafkaConfig.AzpubsubValidateTokenInMinutes
    val zkUrl = if(props.containsKey(AzPubSubAclAuthorizer.ZkUrlProp)) props.get(AzPubSubAclAuthorizer.ZkUrlProp).toString else kafkaConfig.zkConnect
    val zkConnectionTimeoutMs = if(props.containsKey(AzPubSubAclAuthorizer.ZkConnectionTimeOutProp)) props.get(AzPubSubAclAuthorizer.ZkConnectionTimeOutProp).toString.toInt else kafkaConfig.zkConnectionTimeoutMs
    val zkSessionTimeOutMs = if(props.containsKey(AzPubSubAclAuthorizer.ZkSessionTimeOutProp)) props.get(AzPubSubAclAuthorizer.ZkSessionTimeOutProp).toString.toInt else kafkaConfig.zkSessionTimeoutMs
    val zkMaxInFlightRequests = if(props.containsKey(AzPubSubAclAuthorizer.ZkMaxInFlightRequests)) props.get(AzPubSubAclAuthorizer.ZkMaxInFlightRequests).toString.toInt else kafkaConfig.zkMaxInFlightRequests

    initializeZookeeperClient(kafkaConfig, zkUrl, zkConnectionTimeoutMs, zkSessionTimeOutMs, zkMaxInFlightRequests)
  }

   private [auth] def initializeZookeeperClient(kafkaConfig: KafkaConfig, zkUrl: String, zkConnectionTimeoutMs: Int, zkSessionTimeOutMs: Int, zkMaxInFlightRequests: Int) = {
    val time = Time.SYSTEM
    info("zkUrl: " + zkUrl + "; kafkaConfig.zkEnableSecureAcls: " + kafkaConfig.zkEnableSecureAcls);
    zkClient = KafkaZkClient(zkUrl, kafkaConfig.zkEnableSecureAcls, zkSessionTimeOutMs, zkConnectionTimeoutMs,
      zkMaxInFlightRequests, time, "kafka.security", "AzPubSubAzPubSubAclAuthorizer", Option("test"))
    zkClient.createAclPaths()

    startZkChangeListeners()
    loadCache()
  }

  /**
    * authorizing each request.
    * if the principal type is SAML token, we need to ensure the token is not expired, despite of the resource type being accessed.
    * if the request is accessing topic resource, we will do topic authorization; for other type of source,like CLUSTER,
    * the request should be coming from cluster internal, this kinds of requests should be always allowed.
    * If the request is coming from channels like PLAINTEXT or SSL, the principal created by Kafka is "User:ANONYMOUS",
    * we need to make sure in AzPubSubRegistrar (Topics-Prod.ini) the ANONYMOUS user is granted with appropriate permission.
    *
    * @param session the current connection context, authorizationId is saved in the session
    * @param operation Operation type, options are: Read, Write, Delete, Describe, All
    * @param resource Resource being access, in format of ResourceType:ResourceName, e.g., Topic:kattesttopic; or Cluster:
    * @return
    */
  override def authorize(session: Session, operation: Operation, resource: Resource): Boolean = {

    debug(s"authorize - resource.resourceType: ${resource.resourceType.name}; principal: ${Try(session.principal.getName).getOrElse("Empty principal name")}, Operation: ${operation.name}, principal type: ${Try(session.principal.getPrincipalType).getOrElse("Empty principal type")};")

    markMetricsForAclAuthorizationRequest

    if(operation == Describe) {
      debug(s"Topic ${resource.name}  metadata description is allowed , authorized, Client Address: ${session.clientAddress.getHostAddress}")
      return true
    }

    resource.resourceType match {
      case Topic => {
        if(topicsWhiteListed.contains(resource.name)) {
          debug(s"Topic ${resource.name} is white listed, authorized. Client Address: ${session.clientAddress.getHostAddress}")
          return true
        }

        val acls = getAcls(resource) ++ getAcls(new Resource(resource.resourceType, Resource.WildCardResource, PatternType.LITERAL))
        debug(s"Acls read from Zookeeper, length: ${acls.size}.")

        session.principal.getPrincipalType match {

          /**
            * If principal is regular User, including anonymous use, directly match the acl configured in Topic-Prod.ini.
            * As mentioned above, for regular PLAINTEXT ans SSL channel, the principal is: User:ANONYMOUS
            *
            */
          case KafkaPrincipal.USER_TYPE => aclMatch(operation, resource, session.principal, session.clientAddress.getHostAddress, Allow, acls)

          /**
            * If the connection session is using SAML2 token, check if any claim of the token has permission to the topic, which is configured in Topic-Prod.ini
            */
          case KafkaPrincipal.TOKEN_TYPE => {

            /**
              * Check if the current session is using SAML token as authorization id.
              * If it is, retrieve the token and deserialize the JSON string into object of Token class
              * For dsts token, KafkaPrincipal = new KafkaPrincipal(principalType = "Token", name = "AzPubSubOAuthTBearerToken_JSON_string");
              */
            Json.parseStringAs[AzPubSubOAuthBearerToken](session.principal.getName) match {
              case Left(ex) => {error(s"Failed to parse token into AzPubSubOAuthBearerToken for client IP address: ${session.clientAddress.toString}, error: ${ex.getMessage}")}
              case Right(token) => {
                return authorizeTokenToResource(session, operation, resource, acls, token)
              }
            }

            return false
          }
          case _ => {
            warn(s"unknown principal rejected: ${session.principal}, accessing resource: ${resource}, operation: ${operation}")

            return false
          }
        }
      }
      case Group => {
        return true
      }
      case TransactionalId => {
        return true
      }
      case DelegationToken => {
        return true
      }
      case _ => {

        /**
          * If the client is trying to access other resources like "Cluster/Group/DelegationToken/TxnId",
          * This kind of requests should be always coming from brokers of the current cluster; otherwise, the request should be rejected.
          */

        debug(s"session client address: ${session.clientAddress.getHostAddress}")
        if(!brokerHosts.contains(session.clientAddress.getHostAddress)) {
          val allBrokers = zkClient.getAllBrokersInCluster
          allBrokers.foreach(b => b.endPoints.foreach(e => {
            brokerHosts += InetAddress.getByName(e.host).getHostAddress
            debug(s"Kafka broker host : ${e.host}")
          }))
        }

        if(!brokerHosts.contains (session.clientAddress.getHostAddress) ){
          warn(s"Client is not broker and accessing ${resource.resourceType} rejected: ${session.clientAddress.getHostAddress}")
          return false
        }

        debug(s"Client is broker and accessing ${resource.resourceType} allowed: ${session.clientAddress.getHostAddress}")
        return true
      }
    }
  }


  private [auth] def ticksToDate(ticks: Long): Date = {
    new Date((ticks - AzPubSubAclAuthorizer.UnixEpochTimeZeroTicks) / 10000)
  }

  private [auth] def authorizeTokenToResource(session: Session, operation: Operation, resource: Resource, acls: Set[Acl], token: AzPubSubOAuthBearerToken) : Boolean = {
    val validFrom = ticksToDate(token.getValidFromTicks)
    val validTo = ticksToDate(token.getValidToTicks)

    /**
      * If there's a token, then validate if the token is still valid - token not expired.
      */
    val formatter = new SimpleDateFormat(AzPubSubAclAuthorizer.Saml2TokenTimeFormatter, Locale.ENGLISH)
    formatter.setTimeZone(TimeZone.getTimeZone("UTC"))
    val utcNowString = formatter.format(new Date)
    val currentMoment = formatter.parse(utcNowString)

    if (!cacheTokenLastValidatedTime.contains(token.getTokenId)) {
      cacheTokenLastValidatedTime += (token.getTokenId -> new Date(0L))
    }

    /**
      * TOKEN cache one --- after periodToValidateTokenInMinutes, server will re-authenticate the TOKEN again in case the token gets recalled
      */
    if (cacheTokenLastValidatedTime(token.getTokenId).toInstant.plus(periodToValidateTokenInMinutes, ChronoUnit.MINUTES).isBefore(currentMoment.toInstant)) {
      if (false == tokenAuthenticator.validateWithTokenExpiredAllowed(token.getOriginalBase64Token)) {
        error(s"token validation failed, token: ${token.getOriginalBase64Token}")
        return false
      }
      cacheTokenLastValidatedTime(token.getTokenId) = currentMoment
    }

    debug(s"Token is valid. ValidFrom: ${validFrom}, ValidTo: ${validTo}. Topic to access: ${resource.name}, Client Address: ${session.clientAddress.getHostAddress}")

    for (i <- 0 until token.getClaims.size()) {
      val claim = token.getClaims.get(i)
      debug(s"Validating if the claim from json token is allowed to access the resource: ${claim.getValue}")
      val principal = new KafkaPrincipal(KafkaPrincipal.ROLE_TYPE, claim.getValue)
      if (aclMatch(operation, resource, principal, session.clientAddress.getHostAddress, Allow, acls)) {
        debug(s"Authorization for ${principal} operation ${operation} on resource ${resource} succeeded.")
        markMetricsForTokenIsAuthorized
        return true
      }
    }

    warn(s"The token doesn't have any role permitted to access the particular topic ${resource.name}.")
    markMetricsForUnauthorizedToken
    return false
  }

  private def markMetricsForTokenIsAuthorized() = {
    val meterValidToken = newMeter(AzPubSubAclAuthorizer.TopicAuthorizationUsingTokenSuccessfulRateMs, "validtoken", TimeUnit.SECONDS)
    meterValidToken.mark()
  }

  private def markMetricsForAclAuthorizationRequest() = {
    val meterAclAuthorizationRequest = newMeter(AzPubSubAclAuthorizer.AzPubSubAclAuthorizingRequest, "aclauthorizationrequest", TimeUnit.SECONDS)
    meterAclAuthorizationRequest.mark()
  }

  private def markMetricsForUnauthorizedToken() = {
    val meterUnauthorizedToken = newMeter(AzPubSubAclAuthorizer.TokenNotAuthorizedForTopicRateMs, "unauthorizedtoken", TimeUnit.SECONDS)
    meterUnauthorizedToken.mark()
  }

  protected def aclMatch(operations: Operation, resource: Resource, principal: KafkaPrincipal, host: String, permissionType: PermissionType, acls: Set[Acl]): Boolean = {
    acls.find { acl =>
      acl.permissionType == permissionType &&
        (acl.principal == principal
          || (principal.getPrincipalType == KafkaPrincipal.USER_TYPE && acl.principal == KafkaPrincipal.WildCardUserTypePrincipal)
          || (principal.getPrincipalType == KafkaPrincipal.ROLE_TYPE && acl.principal == KafkaPrincipal.WildCardRoleTypePrincipal ) ) &&
        (operations == acl.operation || acl.operation == All) &&
        (acl.host == host || acl.host == Acl.WildCardHost)
    }.exists {
      acl =>
        debug(s"operation = $operations on resource = $resource from host = $host is $permissionType based on acl = $acl")
        true
    }
  }

  object AclChangedNotificationHandler extends NotificationHandler {
    override def processNotification(notificationMessage: Array[Byte]) {
      val resource: Resource = Resource.fromString(new String(notificationMessage, StandardCharsets.UTF_8))
      inWriteLock(lock) {
        val versionedAcls = getAclsFromZk(resource)
        updateCache(resource, versionedAcls)
      }
    }
  }

  override def addAcls(acls: Set[Acl], resource: Resource) {
    if (acls != null && acls.nonEmpty) {
      inWriteLock(lock) {
        updateResourceAcls(resource) { currentAcls =>
          currentAcls ++ acls
        }
      }
    }
  }

  override def removeAcls(aclsTobeRemoved: Set[Acl], resource: Resource): Boolean = {
    inWriteLock(lock) {
      updateResourceAcls(resource) { currentAcls =>
        currentAcls -- aclsTobeRemoved
      }
    }
  }

  override def removeAcls(resource: Resource): Boolean = {
    inWriteLock(lock) {
      val result = zkClient.deleteResource(resource)
      updateCache(resource, VersionedAcls(Set(), 0))
      updateAclChangedFlag(resource)
      result
    }
  }

  override def getAcls(resource: Resource): Set[Acl] = {
    inReadLock(lock) {
      aclCache.get(resource).map(_.acls).getOrElse(Set.empty[Acl])
    }
  }

  override def getAcls(principal: KafkaPrincipal): Map[Resource, Set[Acl]] = {
    inReadLock(lock) {
      aclCache.mapValues { versionedAcls =>
        versionedAcls.acls.filter(_.principal == principal)
      }.filter { case (_, acls) =>
        acls.nonEmpty
      }.toMap
    }
  }

  override def getAcls(): Map[Resource, Set[Acl]] = {
    inReadLock(lock) {
      aclCache.mapValues(_.acls).toMap
    }
  }

  def close() {
    if (aclChangeListener != null) aclChangeListener.close()
    if (zkClient != null) zkClient.close()
  }


  private[auth] def startZkChangeListeners(): Unit = {
    aclChangeListener = new ZkNodeChangeNotificationListener(zkClient, LiteralAclChangeStore.aclChangePath, ZkAclChangeStore.SequenceNumberPrefix, AclChangedNotificationHandler)
    aclChangeListener.init()
  }

  private def updateCache(resource: Resource, versionedAcls: VersionedAcls) {
    inWriteLock(lock) {
      if (versionedAcls.acls.nonEmpty) {
        aclCache.put(resource, versionedAcls)
      } else {
        aclCache.remove(resource)
      }
    }
  }

  private def getAclsFromZk(resource: Resource): VersionedAcls = {
    val acls = zkClient.getVersionedAclsForResource(resource)
    VersionedAcls(acls.acls, acls.zkVersion)
  }

  private [auth] def loadCache()  {
    inWriteLock(lock) {
      val resourceTypes = zkClient.getResourceTypes(PatternType.LITERAL)
      for (rType <- resourceTypes) {
        val resourceType = ResourceType.fromString(rType)
        val resourceNames = zkClient.getResourceNames(PatternType.LITERAL, resourceType)
        for (resourceName <- resourceNames) {
          val versionedAcls = getAclsFromZk(Resource(resourceType, resourceName, PatternType.LITERAL))
          updateCache(new Resource(resourceType, resourceName, PatternType.LITERAL), versionedAcls)
        }
      }
    }
  }

  private def updateAclChangedFlag(resource: Resource) {
    zkClient.createAclChangeNotification(resource)
  }

  /**
    * Safely updates the resources ACLs by ensuring reads and writes respect the expected zookeeper version.
    * Continues to retry until it successfully updates zookeeper.
    *
    * Returns a boolean indicating if the content of the ACLs was actually changed.
    *
    * @param resource the resource to change ACLs for
    * @param getNewAcls function to transform existing acls to new ACLs
    * @return boolean indicating if a change was made
    */
  private def updateResourceAcls(resource: Resource)(getNewAcls: Set[Acl] => Set[Acl]): Boolean = {
    var currentVersionedAcls =
      if (aclCache.contains(resource))
        getAclsFromCache(resource)
      else
        getAclsFromZk(resource)
    var newVersionedAcls: VersionedAcls = null
    var writeComplete = false
    var retries = 0
    while (!writeComplete && retries <= maxUpdateRetries) {
      val newAcls = getNewAcls(currentVersionedAcls.acls)
      val (updateSucceeded, updateVersion) =
        if (newAcls.nonEmpty) {
          zkClient.conditionalSetOrCreateAclsForResource(resource, newAcls, currentVersionedAcls.zkVersion)
        } else {
          trace(s"Deleting path for $resource because it had no ACLs remaining")
          (zkClient.conditionalDelete(resource, currentVersionedAcls.zkVersion), 0)
        }

      if (!updateSucceeded) {
        trace(s"Failed to update ACLs for $resource. Used version ${currentVersionedAcls.zkVersion}. Reading data and retrying update.")
        Thread.sleep(backoffTime)
        currentVersionedAcls = getAclsFromZk(resource)
        retries += 1
      } else {
        newVersionedAcls = VersionedAcls(newAcls, updateVersion)
        writeComplete = updateSucceeded
      }
    }

    if(!writeComplete)
      throw new IllegalStateException(s"Failed to update ACLs for $resource after trying a maximum of $maxUpdateRetries times")

    if (newVersionedAcls.acls != currentVersionedAcls.acls) {
      info(s"Updated ACLs for $resource to ${newVersionedAcls.acls} with version ${newVersionedAcls.zkVersion}")
      updateCache(resource, newVersionedAcls)
      updateAclChangedFlag(resource)
      true
    } else {
      info(s"Updated ACLs for $resource, no change was made")
      updateCache(resource, newVersionedAcls)
      false
    }
  }

  private def backoffTime = {
    retryBackoffMs + Random.nextInt(retryBackoffJitterMs)
  }

  private def getAclsFromCache(resource: Resource): VersionedAcls = {
    aclCache.getOrElse(resource, throw new IllegalArgumentException(s"ACLs do not exist in the cache for resource $resource"))
  }
}
