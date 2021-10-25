package com.microsoft.azpubsub.security.auth

import com.typesafe.scalalogging.Logger
import com.yammer.metrics.core.{Meter, MetricName}
import kafka.metrics.KafkaMetricsGroup
import kafka.security.authorizer.AclAuthorizer
import kafka.utils.{Logging, Pool}
import org.apache.kafka.common.resource.ResourceType.TOPIC
import org.apache.kafka.common.security.auth.{KafkaPrincipal, SecurityProtocol}
import org.apache.kafka.common.utils.Utils
import org.apache.kafka.server.authorizer.{Action, AuthorizableRequestContext, AuthorizationResult}

import java.net.InetAddress
import java.util
import java.util.concurrent._
import scala.jdk.CollectionConverters._

/*
 * AzPubSub ACL Authorizer to handle the certificate & role based principal type
 */
class AzPubSubAclAuthorizer extends AclAuthorizer with Logging {
  private[security] val aclAuthorizerLogger = Logger("kafka.authorizer.logger")

  private val authorizerStats = new AuthorizerStats

  private var authZConfig: AuthZConfig = null

  override def configure(javaConfigs: util.Map[String, _]): Unit = {
    val config = AzPubSubConfig.fromProps(javaConfigs)
    authZConfig = Utils.newInstance(config.getString(AzPubSubConfig.AUTHZ_CLASS_CONFIG), classOf[AuthZConfig])
    authZConfig.configure()
    super.configure(javaConfigs)
  }

  override def authorize(requestContext: AuthorizableRequestContext, actions: util.List[Action]): util.List[AuthorizationResult] = {
    actions.asScala.map { action => this.authorizeAction(requestContext, action) }.asJava
  }

  private def authorizeAction(requestContext: AuthorizableRequestContext, action: Action): AuthorizationResult = {
    val resource = action.resourcePattern
    val sessionPrincipal = requestContext.principal
    var principalName = sessionPrincipal.getName
    val deniedUsers = authZConfig.getDeniedUsers

    if (classOf[AzPubSubPrincipal] == sessionPrincipal.getClass) {
      val principal = sessionPrincipal.asInstanceOf[AzPubSubPrincipal]
      principalName = principal.getPrincipalName
    }

    if (resource.resourceType == TOPIC && authZConfig.isDisabled(resource.name)) {
      aclAuthorizerLogger.debug(s"AuthZ is disabled for resource: $resource")
      authorizerStats.allStats(action, principalName).successRate.mark()
      authorizerStats.allStats(action, principalName).disabledRate.mark()
      return AuthorizationResult.ALLOWED
    }

    def getClaimRequestContext(requestContext: AuthorizableRequestContext, claimPrincipal: KafkaPrincipal): AuthorizableRequestContext = {
      new AuthorizableRequestContext {
        override def clientId(): String = requestContext.clientId
        override def requestType(): Int = requestContext.requestType
        override def listenerName(): String = requestContext.listenerName
        override def clientAddress(): InetAddress = requestContext.clientAddress
        override def principal(): KafkaPrincipal = claimPrincipal
        override def securityProtocol(): SecurityProtocol = requestContext.securityProtocol
        override def correlationId(): Int = requestContext.correlationId
        override def requestVersion(): Int = requestContext.requestVersion
      }
    }

    if (classOf[AzPubSubPrincipal] == sessionPrincipal.getClass) {
      val principal = sessionPrincipal.asInstanceOf[AzPubSubPrincipal]
      for (role <- principal.getRoles.asScala) {
        val deniedRole = deniedUsers.contains(role)
        val claimPrincipal = new KafkaPrincipal(principal.getPrincipalType(), role)
        val claimRequestContext = getClaimRequestContext(requestContext, claimPrincipal)
        if (!deniedRole && super.authorize(claimRequestContext, List(action).asJava).asScala.head == AuthorizationResult.ALLOWED) {
          authorizerStats.allStats(action, claimPrincipal.getName).successRate.mark()
          return AuthorizationResult.ALLOWED
        }
        if (deniedRole) {
          aclAuthorizerLogger.warn(s"Principal role $role is in the denied.users")
        }
      }
    } else if (!deniedUsers.contains(requestContext.principal().getName) && super.authorize(requestContext, List(action).asJava).asScala.head == AuthorizationResult.ALLOWED) {
      authorizerStats.allStats(action, principalName).successRate.mark()
      return AuthorizationResult.ALLOWED
    }

    authorizerStats.allStats(action, principalName).failureRate.mark()
    return AuthorizationResult.DENIED
  }
}

class AuthorizerMetrics(tags: scala.collection.Map[String, String]) extends KafkaMetricsGroup {

  case class MeterWrapper(metricType: String, eventType: String) {
    @volatile private var lazyMeter: Meter = _
    private val meterLock = new Object

    def meter(): Meter = {
      var meter = lazyMeter
      if (meter == null) {
        meterLock synchronized {
          meter = lazyMeter
          if (meter == null) {
            meter = newMeter(metricType, eventType, TimeUnit.SECONDS, tags)
            lazyMeter = meter
          }
        }
      }
      meter
    }

    if (tags.isEmpty) // greedily initialize the general topic metrics
      meter()
  }

  // an internal map for "lazy initialization" of certain metrics
  private val metricTypeMap = new Pool[String, MeterWrapper]
  metricTypeMap.putAll(Map(
    AuthorizerStats.SuccessPerSec -> MeterWrapper(AuthorizerStats.SuccessPerSec, "success"),
    AuthorizerStats.FailurePerSec -> MeterWrapper(AuthorizerStats.FailurePerSec, "failure"),
    AuthorizerStats.DisabledPerSec -> MeterWrapper(AuthorizerStats.DisabledPerSec, "success"),
  ).asJava)

  def successRate = metricTypeMap.get(AuthorizerStats.SuccessPerSec).meter()

  def failureRate = metricTypeMap.get(AuthorizerStats.FailurePerSec).meter()

  def disabledRate = metricTypeMap.get(AuthorizerStats.DisabledPerSec).meter()

  override def metricName(name: String, metricTags: scala.collection.Map[String, String]): MetricName = {
    explicitMetricName("azpubsub.security", "AuthorizerMetrics", name, metricTags)
  }
}

object AuthorizerStats {
  val SuccessPerSec = "AuthorizerSuccessPerSec"
  val FailurePerSec = "AuthorizerFailurePerSec"
  val DisabledPerSec = "AuthorizerDisabledPerSec"
}

class AuthorizerStats {
  private val stats = new Pool[String, AuthorizerMetrics]

  def allStats(action: Action, identity: String): AuthorizerMetrics = {
    val resourceName = action.resourcePattern.name
    val resourceType = action.resourcePattern.resourceType.toString
    val operation = action.operation.toString

    val identityStr = identity.replaceAll(",", "_").replaceAll("\\\\\\\\", "_").replaceAll("\\\\", "_")
    val tags: scala.collection.Map[String, String] = Map("resource-name" -> resourceName, "resource-type" -> resourceType,
      "operation" -> operation, "identity" -> identityStr)

    val key = resourceName + resourceType + operation + identityStr
    if (!stats.contains(key)) {
      stats.put(key, new AuthorizerMetrics(tags))
    }

    stats.get(key)
  }
}