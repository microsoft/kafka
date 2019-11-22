import java.util
import java.util.Date
import java.util.concurrent.{ScheduledExecutorService, TimeUnit}

import com.yammer.metrics.core.Clock
import kafka.metrics.KafkaMetricsGroup
import kafka.network.RequestChannel.Session
import kafka.security.auth.{Acl, All, Allow, AzPubSubAclAuthorizer, Resource, Topic}
import kafka.server.KafkaConfig
import kafka.zk.KafkaZkClient
import kafka.zookeeper.ZooKeeperClient
import org.apache.kafka.common.resource.PatternType

import scala.collection.mutable
import org.apache.kafka.common.security.auth.KafkaPrincipal
import org.apache.kafka.common.utils.{SystemTime, Time}
import org.easymock.EasyMock
import org.easymock.EasyMock._
import org.junit.{Ignore, Test}
import org.junit.runner.RunWith
import org.powermock.api.easymock.PowerMock
import org.powermock.api.support.membermodification.MemberMatcher
import org.powermock.core.classloader.annotations.{PowerMockIgnore, PrepareForTest}
import org.powermock.modules.junit4.PowerMockRunner
import org.powermock.reflect.Whitebox
import org.powermock.api.support.membermodification.MemberModifier.suppress
import org.slf4j.Logger

import scala.collection.JavaConverters._


object AzPubSubAclAuthorizerTest{
  val mockPositiveTokenValidator = classOf[mockPositiveTokenValidator].getName
  val mockNonExistingTokenValidator = "notExistingClass"
}

@RunWith(classOf[PowerMockRunner])
@PrepareForTest(Array(classOf[org.slf4j.LoggerFactory], classOf[AzPubSubAclAuthorizer], classOf[KafkaMetricsGroup], classOf[KafkaZkClient], classOf[ZooKeeperClient]))
@PowerMockIgnore(Array("javax.management.*"))
class AzPubSubAclAuthorizerTest {

      @Test
      def testConfigurePositive(): Unit = {
            val authorizer: AzPubSubAclAuthorizer = EasyMock.partialMockBuilder(classOf[AzPubSubAclAuthorizer])
              .addMockedMethod("startZkChangeListeners")
              .addMockedMethod("loadCache")
              .createMock()

            suppress(MemberMatcher.methodsDeclaredIn(classOf[KafkaZkClient]))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "newGauge"))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "newMeter"))
            suppress(MemberMatcher.method(classOf[ZooKeeperClient], "newGauge"))
            suppress(MemberMatcher.method(classOf[ZooKeeperClient], "newMeter"))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "initializeZookeeperClient", classOf[KafkaConfig], classOf[String], classOf[Int], classOf[Int], classOf[Int]))

            EasyMock.replay(authorizer)

            val javaConfigs = new util.HashMap[String, String]()
            javaConfigs.put(KafkaConfig.AzpubsubTokenValidatorClassProp, AzPubSubAclAuthorizerTest.mockPositiveTokenValidator)
            javaConfigs.put(KafkaConfig.AzpubsubValidateTokenInMinutesProp, "60")
            javaConfigs.put(AzPubSubAclAuthorizer.ZkUrlProp, "localhost:2181")
            javaConfigs.put(AzPubSubAclAuthorizer.ZkSessionTimeOutProp, "50")
            javaConfigs.put(AzPubSubAclAuthorizer.ZkMaxInFlightRequests, "40")
            javaConfigs.put(KafkaConfig.ZkConnectProp, "loalhost:2181")
            javaConfigs.put(KafkaConfig.AzPubSubTopicWhiteListProp, "topic1,topic2")

            authorizer.configure(javaConfigs)

            EasyMock.verify(authorizer)
      }

      @Test(expected = classOf[ClassNotFoundException])
      def testConfigureNegative(): Unit = {
            val authorizer: AzPubSubAclAuthorizer = EasyMock.partialMockBuilder(classOf[AzPubSubAclAuthorizer])
              .addMockedMethod("startZkChangeListeners")
              .createMock()

            suppress(MemberMatcher.methodsDeclaredIn(classOf[KafkaZkClient]))
            suppress(MemberMatcher.constructorsDeclaredIn(classOf[ZooKeeperClient]))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "newGauge"))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "newMeter"))
            suppress(MemberMatcher.method(classOf[ZooKeeperClient], "newGauge"))
            suppress(MemberMatcher.method(classOf[ZooKeeperClient], "newMeter"))

            EasyMock.replay(authorizer)

            val javaConfigs = new util.HashMap[String, String]()
            javaConfigs.put(KafkaConfig.AzpubsubTokenValidatorClassProp, AzPubSubAclAuthorizerTest.mockNonExistingTokenValidator)
            javaConfigs.put(KafkaConfig.AzpubsubValidateTokenInMinutesProp, "60")
            javaConfigs.put(AzPubSubAclAuthorizer.ZkUrlProp, "localhost:2181")
            javaConfigs.put(AzPubSubAclAuthorizer.ZkSessionTimeOutProp, "50")
            javaConfigs.put(AzPubSubAclAuthorizer.ZkMaxInFlightRequests, "40")
            javaConfigs.put(KafkaConfig.ZkConnectProp, "loalhost:2181")

            authorizer.configure(javaConfigs)
      }



      @Test
      def testAzPubSubAclAuthorizerAuthorizeTokenPositive(): Unit = {
            val tokenJsonString = "{\"tokenId\":\"_51d8f7bb-3e25-46d1-a617-cf3e49393a28\",\"validFromTicks\":637099656400310000,\"validToTicks\":699099674400310000,\"originalBase64Token\":\"PEFzc2VydGlvbiBqb0E4QUFBQUFxNHpEQU5CZ2txaGtpRzl3MEJBUXNGQURDQml6RUx0ZW1lbnQ+PC9Bc3NlcnRpb24+\",\"claims\":[{\"claimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier\",\"issuer\":\"realm://dsts.core.azure-test.net/\",\"originalIssuer\":\"realm://dsts.core.azure-test.net/\",\"label\":null,\"nameClaimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"roleClaimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"value\":\"zookeeperclienttest-useast.core.windows.net\",\"valueType\":\"http://www.w3.org/2001/XMLSchema#string\"},{\"claimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"issuer\":\"realm://dsts.core.azure-test.net/\",\"originalIssuer\":\"realm://dsts.core.azure-test.net/\",\"label\":null,\"nameClaimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"roleClaimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"value\":\"zookeeperclienttest-useast.core.windows.net\",\"valueType\":\"http://www.w3.org/2001/XMLSchema#string\"},{\"claimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"issuer\":\"realm://dsts.core.azure-test.net/\",\"originalIssuer\":\"realm://dsts.core.azure-test.net/\",\"label\":null,\"nameClaimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"roleClaimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"value\":\"ToAuthenticateAzPubSub\",\"valueType\":\"http://www.w3.org/2001/XMLSchema#string\"},{\"claimType\":\"http://sts.msft.net/computer/DeviceGroup\",\"issuer\":\"realm://dsts.core.azure-test.net/\",\"originalIssuer\":\"realm://dsts.core.azure-test.net/\",\"label\":null,\"nameClaimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"roleClaimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"value\":\"AzPubSub.Autopilot.Co4,AzPubSub.Autopilot.Bn2\",\"valueType\":\"http://www.w3.org/2001/XMLSchema#string\"},{\"claimType\":\"http://schemas.microsoft.com/accesscontrolservice/2010/07/claims/identityprovider\",\"issuer\":\"realm://dsts.core.azure-test.net/\",\"originalIssuer\":\"realm://dsts.core.azure-test.net/\",\"label\":null,\"nameClaimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"roleClaimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"value\":\"realm://dsts.core.azure-test.net/\",\"valueType\":\"http://www.w3.org/2001/XMLSchema#string\"}]}"

            val principal = new KafkaPrincipal(KafkaPrincipal.TOKEN_TYPE, tokenJsonString)
            val localHost = java.net.InetAddress.getLocalHost
            val session = Session(principal, localHost)
            val resource = Resource(Topic, "testTopic", PatternType.LITERAL)

            val authorizer: AzPubSubAclAuthorizer = EasyMock.partialMockBuilder(classOf[AzPubSubAclAuthorizer])
                  .addMockedMethod("getAcls", classOf[Resource])
                  .createMock()
            val acls = Set(Acl(new KafkaPrincipal("Role", "ToAuthenticateAzPubSub"), Allow, "*", All))
            EasyMock.expect(authorizer.getAcls(isA(classOf[Resource]))).andReturn(acls).anyTimes()
            suppress(MemberMatcher.methodsDeclaredIn(classOf[KafkaMetricsGroup]))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "newGauge"))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "markMetricsForAclAuthorizationRequest"))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "markMetricsForInvalidFromDateInToken"))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "markMetricsForInvalidToDateInToken"))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "markMetricsForUnauthorizedToken"))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "markMetricsForTokenIsAuthorized"))

            val logger: org.slf4j.Logger = EasyMock.mock(classOf[org.slf4j.Logger])

            EasyMock.expect(logger.info(isA(classOf[String]))).andVoid().anyTimes()
            EasyMock.expect(logger.debug(isA(classOf[String]))).andVoid().anyTimes()
            EasyMock.expect(logger.warn(isA(classOf[String]))).andVoid().anyTimes()

            val cache = new mutable.HashMap[String, Date]
            val validator = new mockPositiveTokenValidator

            Whitebox.setInternalState(authorizer, "cacheTokenLastValidatedTime", cache.asInstanceOf[Any])
            Whitebox.setInternalState(authorizer, "tokenAuthenticator", validator.asInstanceOf[Any])
            Whitebox.setInternalState(authorizer, "topicsWhiteListed", mutable.HashSet[String]("topic1", "topic2").asInstanceOf[Any])

            EasyMock.replay(authorizer)

            assertAuthorizationAndTokenCache(true, session, resource, authorizer, cache)
            EasyMock.verify(authorizer)
      }

      @Test
      def testAzPubSubAclAuthorizerAuthorizeTokenNoAuthorizedClaim(): Unit = {
            val tokenJsonString = "{\"tokenId\":\"_51d8f7bb-3e25-46d1-a617-cf3e49393a28\",\"validFromTicks\":637099656400310000,\"validToTicks\":699099674400310000,\"originalBase64Token\":\"PEFzc2VydGlvbiBqb0E4QUFBQUFxNHpEQU5CZ2txaGtpRzl3MEJBUXNGQURDQml6RUx0ZW1lbnQ+PC9Bc3NlcnRpb24+\",\"claims\":[{\"claimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier\",\"issuer\":\"realm://dsts.core.azure-test.net/\",\"originalIssuer\":\"realm://dsts.core.azure-test.net/\",\"label\":null,\"nameClaimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"roleClaimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"value\":\"zookeeperclienttest-useast.core.windows.net\",\"valueType\":\"http://www.w3.org/2001/XMLSchema#string\"},{\"claimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"issuer\":\"realm://dsts.core.azure-test.net/\",\"originalIssuer\":\"realm://dsts.core.azure-test.net/\",\"label\":null,\"nameClaimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"roleClaimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"value\":\"zookeeperclienttest-useast.core.windows.net\",\"valueType\":\"http://www.w3.org/2001/XMLSchema#string\"},{\"claimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"issuer\":\"realm://dsts.core.azure-test.net/\",\"originalIssuer\":\"realm://dsts.core.azure-test.net/\",\"label\":null,\"nameClaimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"roleClaimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"value\":\"ToAuthenticate\",\"valueType\":\"http://www.w3.org/2001/XMLSchema#string\"},{\"claimType\":\"http://sts.msft.net/computer/DeviceGroup\",\"issuer\":\"realm://dsts.core.azure-test.net/\",\"originalIssuer\":\"realm://dsts.core.azure-test.net/\",\"label\":null,\"nameClaimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"roleClaimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"value\":\"AzPubSub.Autopilot.Co4,AzPubSub.Autopilot.Bn2\",\"valueType\":\"http://www.w3.org/2001/XMLSchema#string\"},{\"claimType\":\"http://schemas.microsoft.com/accesscontrolservice/2010/07/claims/identityprovider\",\"issuer\":\"realm://dsts.core.azure-test.net/\",\"originalIssuer\":\"realm://dsts.core.azure-test.net/\",\"label\":null,\"nameClaimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"roleClaimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"value\":\"realm://dsts.core.azure-test.net/\",\"valueType\":\"http://www.w3.org/2001/XMLSchema#string\"}]}"

            val principal = new KafkaPrincipal(KafkaPrincipal.TOKEN_TYPE, tokenJsonString)
            val localHost = java.net.InetAddress.getLocalHost
            val session = Session(principal, localHost)
            val resource = Resource(Topic, "testTopic", PatternType.LITERAL)

            val authorizer: AzPubSubAclAuthorizer = EasyMock.partialMockBuilder(classOf[AzPubSubAclAuthorizer])
              .addMockedMethod("getAcls", classOf[Resource])
              .createMock()
            val acls = Set(Acl(new KafkaPrincipal("Role", "NotExistingClaim"), Allow, "*", All))
            EasyMock.expect(authorizer.getAcls(isA(classOf[Resource]))).andReturn(acls).anyTimes()
            suppress(MemberMatcher.methodsDeclaredIn(classOf[KafkaMetricsGroup]))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "newGauge"))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "markMetricsForAclAuthorizationRequest"))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "markMetricsForInvalidFromDateInToken"))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "markMetricsForInvalidToDateInToken"))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "markMetricsForUnauthorizedToken"))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "markMetricsForTokenIsAuthorized"))

            val logger: org.slf4j.Logger = EasyMock.mock(classOf[org.slf4j.Logger])

            EasyMock.expect(logger.info(isA(classOf[String]))).andVoid().anyTimes()
            EasyMock.expect(logger.debug(isA(classOf[String]))).andVoid().anyTimes()
            EasyMock.expect(logger.warn(isA(classOf[String]))).andVoid().anyTimes()

            val cache = new mutable.HashMap[String, Date]
            val validator = new mockPositiveTokenValidator

            Whitebox.setInternalState(authorizer, "cacheTokenLastValidatedTime", cache.asInstanceOf[Any])
            Whitebox.setInternalState(authorizer, "tokenAuthenticator", validator.asInstanceOf[Any])
            Whitebox.setInternalState(authorizer, "topicsWhiteListed", mutable.HashSet[String]("topic1", "topic2").asInstanceOf[Any])

            EasyMock.replay(authorizer)

            assertAuthorizationAndTokenCache(false, session, resource, authorizer, cache)
            EasyMock.verify(authorizer)
      }

      @Test
      def testAzPubSubAclAuthorizerAuthorizeTokenInvalidFromDateExpiredTokenAllowed(): Unit = {
            val tokenJsonString = "{\"tokenId\":\"_51d8f7bb-3e25-46d1-a617-cf3e49393a28\",\"validFromTicks\":699099674400309000,\"validToTicks\":699099674400310000,\"originalBase64Token\":\"PEFzc2VydGlvbiBqb0E4QUFBQUFxNHpEQU5CZ2txaGtpRzl3MEJBUXNGQURDQml6RUx0ZW1lbnQ+PC9Bc3NlcnRpb24+\",\"claims\":[{\"claimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier\",\"issuer\":\"realm://dsts.core.azure-test.net/\",\"originalIssuer\":\"realm://dsts.core.azure-test.net/\",\"label\":null,\"nameClaimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"roleClaimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"value\":\"zookeeperclienttest-useast.core.windows.net\",\"valueType\":\"http://www.w3.org/2001/XMLSchema#string\"},{\"claimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"issuer\":\"realm://dsts.core.azure-test.net/\",\"originalIssuer\":\"realm://dsts.core.azure-test.net/\",\"label\":null,\"nameClaimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"roleClaimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"value\":\"zookeeperclienttest-useast.core.windows.net\",\"valueType\":\"http://www.w3.org/2001/XMLSchema#string\"},{\"claimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"issuer\":\"realm://dsts.core.azure-test.net/\",\"originalIssuer\":\"realm://dsts.core.azure-test.net/\",\"label\":null,\"nameClaimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"roleClaimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"value\":\"ToAuthenticate\",\"valueType\":\"http://www.w3.org/2001/XMLSchema#string\"},{\"claimType\":\"http://sts.msft.net/computer/DeviceGroup\",\"issuer\":\"realm://dsts.core.azure-test.net/\",\"originalIssuer\":\"realm://dsts.core.azure-test.net/\",\"label\":null,\"nameClaimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"roleClaimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"value\":\"AzPubSub.Autopilot.Co4,AzPubSub.Autopilot.Bn2\",\"valueType\":\"http://www.w3.org/2001/XMLSchema#string\"},{\"claimType\":\"http://schemas.microsoft.com/accesscontrolservice/2010/07/claims/identityprovider\",\"issuer\":\"realm://dsts.core.azure-test.net/\",\"originalIssuer\":\"realm://dsts.core.azure-test.net/\",\"label\":null,\"nameClaimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"roleClaimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"value\":\"realm://dsts.core.azure-test.net/\",\"valueType\":\"http://www.w3.org/2001/XMLSchema#string\"}]}"

            val principal = new KafkaPrincipal(KafkaPrincipal.TOKEN_TYPE, tokenJsonString)
            val localHost = java.net.InetAddress.getLocalHost
            val session = Session(principal, localHost)
            val resource = Resource(Topic, "testTopic", PatternType.LITERAL)

            val authorizer: AzPubSubAclAuthorizer = EasyMock.partialMockBuilder(classOf[AzPubSubAclAuthorizer])
              .addMockedMethod("getAcls", classOf[Resource])
              .createMock()
            val acls = Set(Acl(new KafkaPrincipal("Role", "ToAuthenticate"), Allow, "*", All))
            EasyMock.expect(authorizer.getAcls(isA(classOf[Resource]))).andReturn(acls).anyTimes()
            suppress(MemberMatcher.methodsDeclaredIn(classOf[KafkaMetricsGroup]))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "newGauge"))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "markMetricsForAclAuthorizationRequest"))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "markMetricsForInvalidFromDateInToken"))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "markMetricsForInvalidToDateInToken"))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "markMetricsForUnauthorizedToken"))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "markMetricsForTokenIsAuthorized"))

            val logger: org.slf4j.Logger = EasyMock.mock(classOf[org.slf4j.Logger])

            EasyMock.expect(logger.info(isA(classOf[String]))).andVoid().anyTimes()
            EasyMock.expect(logger.debug(isA(classOf[String]))).andVoid().anyTimes()
            EasyMock.expect(logger.warn(isA(classOf[String]))).andVoid().anyTimes()

            val cache = new mutable.HashMap[String, Date]
            val validator = new mockPositiveTokenValidator

            Whitebox.setInternalState(authorizer, "cacheTokenLastValidatedTime", cache.asInstanceOf[Any])
            Whitebox.setInternalState(authorizer, "tokenAuthenticator", validator.asInstanceOf[Any])
            Whitebox.setInternalState(authorizer, "topicsWhiteListed", mutable.HashSet[String]("topic1", "topic2").asInstanceOf[Any])

            EasyMock.replay(authorizer)

            assertAuthorizationAndTokenCache(true, session, resource, authorizer, cache)

            EasyMock.verify(authorizer)
      }

      @Test
      def testAzPubSubAclAuthorizerAuthorizeTokenInvalidToDateExpiredTokenAllowed(): Unit = {
            val tokenJsonString = "{\"tokenId\":\"_51d8f7bb-3e25-46d1-a617-cf3e49393a28\",\"validFromTicks\":637099656400310000,\"validToTicks\":637099656400313010,\"originalBase64Token\":\"PEFzc2VydGlvbiBqb0E4QUFBQUFxNHpEQU5CZ2txaGtpRzl3MEJBUXNGQURDQml6RUx0ZW1lbnQ+PC9Bc3NlcnRpb24+\",\"claims\":[{\"claimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier\",\"issuer\":\"realm://dsts.core.azure-test.net/\",\"originalIssuer\":\"realm://dsts.core.azure-test.net/\",\"label\":null,\"nameClaimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"roleClaimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"value\":\"zookeeperclienttest-useast.core.windows.net\",\"valueType\":\"http://www.w3.org/2001/XMLSchema#string\"},{\"claimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"issuer\":\"realm://dsts.core.azure-test.net/\",\"originalIssuer\":\"realm://dsts.core.azure-test.net/\",\"label\":null,\"nameClaimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"roleClaimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"value\":\"zookeeperclienttest-useast.core.windows.net\",\"valueType\":\"http://www.w3.org/2001/XMLSchema#string\"},{\"claimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"issuer\":\"realm://dsts.core.azure-test.net/\",\"originalIssuer\":\"realm://dsts.core.azure-test.net/\",\"label\":null,\"nameClaimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"roleClaimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"value\":\"ToAuthenticate\",\"valueType\":\"http://www.w3.org/2001/XMLSchema#string\"},{\"claimType\":\"http://sts.msft.net/computer/DeviceGroup\",\"issuer\":\"realm://dsts.core.azure-test.net/\",\"originalIssuer\":\"realm://dsts.core.azure-test.net/\",\"label\":null,\"nameClaimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"roleClaimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"value\":\"AzPubSub.Autopilot.Co4,AzPubSub.Autopilot.Bn2\",\"valueType\":\"http://www.w3.org/2001/XMLSchema#string\"},{\"claimType\":\"http://schemas.microsoft.com/accesscontrolservice/2010/07/claims/identityprovider\",\"issuer\":\"realm://dsts.core.azure-test.net/\",\"originalIssuer\":\"realm://dsts.core.azure-test.net/\",\"label\":null,\"nameClaimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"roleClaimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"value\":\"realm://dsts.core.azure-test.net/\",\"valueType\":\"http://www.w3.org/2001/XMLSchema#string\"}]}"

            val principal = new KafkaPrincipal(KafkaPrincipal.TOKEN_TYPE, tokenJsonString)
            val localHost = java.net.InetAddress.getLocalHost
            val session = Session(principal, localHost)
            val resource = Resource(Topic, "testTopic", PatternType.LITERAL)

            val authorizer: AzPubSubAclAuthorizer = EasyMock.partialMockBuilder(classOf[AzPubSubAclAuthorizer])
              .addMockedMethod("getAcls", classOf[Resource])
              .createMock()
            val acls = Set(Acl(new KafkaPrincipal("Role", "ToAuthenticate"), Allow, "*", All))
            EasyMock.expect(authorizer.getAcls(isA(classOf[Resource]))).andReturn(acls).anyTimes()
            suppress(MemberMatcher.methodsDeclaredIn(classOf[KafkaMetricsGroup]))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "newGauge"))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "markMetricsForAclAuthorizationRequest"))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "markMetricsForInvalidFromDateInToken"))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "markMetricsForInvalidToDateInToken"))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "markMetricsForUnauthorizedToken"))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "markMetricsForTokenIsAuthorized"))

            val logger: Logger = EasyMock.mock(classOf[Logger])

            EasyMock.expect(logger.info(isA(classOf[String]))).andVoid().anyTimes()
            EasyMock.expect(logger.debug(isA(classOf[String]))).andVoid().anyTimes()
            EasyMock.expect(logger.warn(isA(classOf[String]))).andVoid().anyTimes()

            val cache = new mutable.HashMap[String, Date]
            val validator = new mockPositiveTokenValidator

            Whitebox.setInternalState(authorizer, "cacheTokenLastValidatedTime", cache.asInstanceOf[Any])
            Whitebox.setInternalState(authorizer, "tokenAuthenticator", validator.asInstanceOf[Any])
            Whitebox.setInternalState(authorizer, "topicsWhiteListed", mutable.HashSet[String]("topic1", "topic2").asInstanceOf[Any])

            EasyMock.replay(authorizer)

            assertAuthorizationAndTokenCache(true, session, resource, authorizer, cache)
            EasyMock.verify(authorizer)
      }

      @Test
      def testAzPubSubAclAuthorizerAuthorizeTokenInvalidFromDateExpiredTokenNotAllowed(): Unit = {
            val tokenJsonString = "{\"tokenId\":\"_51d8f7bb-3e25-46d1-a617-cf3e49393a28\",\"validFromTicks\":699099674400309000,\"validToTicks\":699099674400310000,\"originalBase64Token\":\"PEFzc2VydGlvbiBqb0E4QUFBQUFxNHpEQU5CZ2txaGtpRzl3MEJBUXNGQURDQml6RUx0ZW1lbnQ+PC9Bc3NlcnRpb24+\",\"claims\":[{\"claimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier\",\"issuer\":\"realm://dsts.core.azure-test.net/\",\"originalIssuer\":\"realm://dsts.core.azure-test.net/\",\"label\":null,\"nameClaimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"roleClaimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"value\":\"zookeeperclienttest-useast.core.windows.net\",\"valueType\":\"http://www.w3.org/2001/XMLSchema#string\"},{\"claimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"issuer\":\"realm://dsts.core.azure-test.net/\",\"originalIssuer\":\"realm://dsts.core.azure-test.net/\",\"label\":null,\"nameClaimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"roleClaimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"value\":\"zookeeperclienttest-useast.core.windows.net\",\"valueType\":\"http://www.w3.org/2001/XMLSchema#string\"},{\"claimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"issuer\":\"realm://dsts.core.azure-test.net/\",\"originalIssuer\":\"realm://dsts.core.azure-test.net/\",\"label\":null,\"nameClaimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"roleClaimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"value\":\"ToAuthenticate\",\"valueType\":\"http://www.w3.org/2001/XMLSchema#string\"},{\"claimType\":\"http://sts.msft.net/computer/DeviceGroup\",\"issuer\":\"realm://dsts.core.azure-test.net/\",\"originalIssuer\":\"realm://dsts.core.azure-test.net/\",\"label\":null,\"nameClaimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"roleClaimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"value\":\"AzPubSub.Autopilot.Co4,AzPubSub.Autopilot.Bn2\",\"valueType\":\"http://www.w3.org/2001/XMLSchema#string\"},{\"claimType\":\"http://schemas.microsoft.com/accesscontrolservice/2010/07/claims/identityprovider\",\"issuer\":\"realm://dsts.core.azure-test.net/\",\"originalIssuer\":\"realm://dsts.core.azure-test.net/\",\"label\":null,\"nameClaimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"roleClaimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"value\":\"realm://dsts.core.azure-test.net/\",\"valueType\":\"http://www.w3.org/2001/XMLSchema#string\"}]}"

            val principal = new KafkaPrincipal(KafkaPrincipal.TOKEN_TYPE, tokenJsonString)
            val localHost = java.net.InetAddress.getLocalHost
            val session = Session(principal, localHost)
            val resource = Resource(Topic, "testTopic", PatternType.LITERAL)

            val authorizer: AzPubSubAclAuthorizer = EasyMock.partialMockBuilder(classOf[AzPubSubAclAuthorizer])
              .addMockedMethod("getAcls", classOf[Resource])
              .createMock()
            val acls = Set(Acl(new KafkaPrincipal("Role", "ToAuthenticate"), Allow, "*", All))
            EasyMock.expect(authorizer.getAcls(isA(classOf[Resource]))).andReturn(acls).anyTimes()
            suppress(MemberMatcher.methodsDeclaredIn(classOf[KafkaMetricsGroup]))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "newGauge"))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "markMetricsForAclAuthorizationRequest"))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "markMetricsForInvalidFromDateInToken"))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "markMetricsForInvalidToDateInToken"))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "markMetricsForUnauthorizedToken"))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "markMetricsForTokenIsAuthorized"))

            val logger: org.slf4j.Logger = EasyMock.mock(classOf[org.slf4j.Logger])

            EasyMock.expect(logger.info(isA(classOf[String]))).andVoid().anyTimes()
            EasyMock.expect(logger.debug(isA(classOf[String]))).andVoid().anyTimes()
            EasyMock.expect(logger.warn(isA(classOf[String]))).andVoid().anyTimes()

            val cache = new mutable.HashMap[String, Date]
            val validator = new mockNegativeTokenValidator

            Whitebox.setInternalState(authorizer, "cacheTokenLastValidatedTime", cache.asInstanceOf[Any])
            Whitebox.setInternalState(authorizer, "tokenAuthenticator", validator.asInstanceOf[Any])
            Whitebox.setInternalState(authorizer, "topicsWhiteListed", mutable.HashSet[String]("topic1", "topic2").asInstanceOf[Any])

            EasyMock.replay(authorizer)

            assertAuthorizationAndTokenCache(false, session, resource, authorizer, cache)

            EasyMock.verify(authorizer)
      }

      private def assertAuthorizationAndTokenCache(authorized: Boolean, session: Session, resource: Resource, authorizer: AzPubSubAclAuthorizer, cache: mutable.HashMap[String, Date]) = {
            assert(authorized == authorizer.authorize(session, All, resource))

            val time1 = new Date().getTime
            assert(cache.size == 1)
            assert(cache.contains("_51d8f7bb-3e25-46d1-a617-cf3e49393a28"))

            val time2 = new Date().getTime
      }

      @Test
      def testAzPubSubAclAuthorizerAuthorizeTokenWhiteListNegative(): Unit = {
            val tokenJsonString = "{\"tokenId\":\"_51d8f7bb-3e25-46d1-a617-cf3e49393a28\",\"validFromTicks\":637099656400310000,\"validToTicks\":699999656400313010,\"originalBase64Token\":\"PEFzc2VydGlvbiBqb0E4QUFBQUFxNHpEQU5CZ2txaGtpRzl3MEJBUXNGQURDQml6RUx0ZW1lbnQ+PC9Bc3NlcnRpb24+\",\"claims\":[{\"claimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier\",\"issuer\":\"realm://dsts.core.azure-test.net/\",\"originalIssuer\":\"realm://dsts.core.azure-test.net/\",\"label\":null,\"nameClaimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"roleClaimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"value\":\"zookeeperclienttest-useast.core.windows.net\",\"valueType\":\"http://www.w3.org/2001/XMLSchema#string\"},{\"claimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"issuer\":\"realm://dsts.core.azure-test.net/\",\"originalIssuer\":\"realm://dsts.core.azure-test.net/\",\"label\":null,\"nameClaimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"roleClaimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"value\":\"zookeeperclienttest-useast.core.windows.net\",\"valueType\":\"http://www.w3.org/2001/XMLSchema#string\"},{\"claimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"issuer\":\"realm://dsts.core.azure-test.net/\",\"originalIssuer\":\"realm://dsts.core.azure-test.net/\",\"label\":null,\"nameClaimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"roleClaimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"value\":\"ToAuthenticate\",\"valueType\":\"http://www.w3.org/2001/XMLSchema#string\"},{\"claimType\":\"http://sts.msft.net/computer/DeviceGroup\",\"issuer\":\"realm://dsts.core.azure-test.net/\",\"originalIssuer\":\"realm://dsts.core.azure-test.net/\",\"label\":null,\"nameClaimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"roleClaimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"value\":\"AzPubSub.Autopilot.Co4,AzPubSub.Autopilot.Bn2\",\"valueType\":\"http://www.w3.org/2001/XMLSchema#string\"},{\"claimType\":\"http://schemas.microsoft.com/accesscontrolservice/2010/07/claims/identityprovider\",\"issuer\":\"realm://dsts.core.azure-test.net/\",\"originalIssuer\":\"realm://dsts.core.azure-test.net/\",\"label\":null,\"nameClaimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"roleClaimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"value\":\"realm://dsts.core.azure-test.net/\",\"valueType\":\"http://www.w3.org/2001/XMLSchema#string\"}]}"

            val principal = new KafkaPrincipal(KafkaPrincipal.TOKEN_TYPE, tokenJsonString)
            val localHost = java.net.InetAddress.getLocalHost
            val session = Session(principal, localHost)
            val resource = Resource(Topic, "testTopic", PatternType.LITERAL)

            val authorizer: AzPubSubAclAuthorizer = EasyMock.partialMockBuilder(classOf[AzPubSubAclAuthorizer])
              .addMockedMethod("getAcls", classOf[Resource])
              .createMock()
            val acls = Set(Acl(new KafkaPrincipal("Role", "NotExistingClaim"), Allow, "*", All))
            EasyMock.expect(authorizer.getAcls(isA(classOf[Resource]))).andReturn(acls).anyTimes()
            suppress(MemberMatcher.methodsDeclaredIn(classOf[KafkaMetricsGroup]))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "newGauge"))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "markMetricsForAclAuthorizationRequest"))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "markMetricsForInvalidFromDateInToken"))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "markMetricsForInvalidToDateInToken"))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "markMetricsForUnauthorizedToken"))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "markMetricsForTokenIsAuthorized"))

            val logger: org.slf4j.Logger = EasyMock.mock(classOf[org.slf4j.Logger])

            EasyMock.expect(logger.info(isA(classOf[String]))).andVoid().anyTimes()
            EasyMock.expect(logger.debug(isA(classOf[String]))).andVoid().anyTimes()
            EasyMock.expect(logger.warn(isA(classOf[String]))).andVoid().anyTimes()

            val cache = new mutable.HashMap[String, Date]
            val validator = new mockPositiveTokenValidator

            Whitebox.setInternalState(authorizer, "cacheTokenLastValidatedTime", cache.asInstanceOf[Any])
            Whitebox.setInternalState(authorizer, "tokenAuthenticator", validator.asInstanceOf[Any])
            Whitebox.setInternalState(authorizer, "topicsWhiteListed", mutable.HashSet[String]("topic1", "topic2").asInstanceOf[Any])

            EasyMock.replay(authorizer)

            assert(false == authorizer.authorize(session, All, resource))
            EasyMock.verify(authorizer)
      }

      @Test
      def testAzPubSubAclAuthorizerAuthorizeTokenWhiteListPositive(): Unit = {
            val tokenJsonString = "{\"tokenId\":\"_51d8f7bb-3e25-46d1-a617-cf3e49393a28\",\"validFromTicks\":637099656400310000,\"validToTicks\":699999656400313010,\"originalBase64Token\":\"PEFzc2VydGlvbiBqb0E4QUFBQUFxNHpEQU5CZ2txaGtpRzl3MEJBUXNGQURDQml6RUx0ZW1lbnQ+PC9Bc3NlcnRpb24+\",\"claims\":[{\"claimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier\",\"issuer\":\"realm://dsts.core.azure-test.net/\",\"originalIssuer\":\"realm://dsts.core.azure-test.net/\",\"label\":null,\"nameClaimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"roleClaimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"value\":\"zookeeperclienttest-useast.core.windows.net\",\"valueType\":\"http://www.w3.org/2001/XMLSchema#string\"},{\"claimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"issuer\":\"realm://dsts.core.azure-test.net/\",\"originalIssuer\":\"realm://dsts.core.azure-test.net/\",\"label\":null,\"nameClaimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"roleClaimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"value\":\"zookeeperclienttest-useast.core.windows.net\",\"valueType\":\"http://www.w3.org/2001/XMLSchema#string\"},{\"claimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"issuer\":\"realm://dsts.core.azure-test.net/\",\"originalIssuer\":\"realm://dsts.core.azure-test.net/\",\"label\":null,\"nameClaimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"roleClaimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"value\":\"ToAuthenticate\",\"valueType\":\"http://www.w3.org/2001/XMLSchema#string\"},{\"claimType\":\"http://sts.msft.net/computer/DeviceGroup\",\"issuer\":\"realm://dsts.core.azure-test.net/\",\"originalIssuer\":\"realm://dsts.core.azure-test.net/\",\"label\":null,\"nameClaimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"roleClaimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"value\":\"AzPubSub.Autopilot.Co4,AzPubSub.Autopilot.Bn2\",\"valueType\":\"http://www.w3.org/2001/XMLSchema#string\"},{\"claimType\":\"http://schemas.microsoft.com/accesscontrolservice/2010/07/claims/identityprovider\",\"issuer\":\"realm://dsts.core.azure-test.net/\",\"originalIssuer\":\"realm://dsts.core.azure-test.net/\",\"label\":null,\"nameClaimType\":\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name\",\"roleClaimType\":\"http://schemas.microsoft.com/ws/2008/06/identity/claims/role\",\"value\":\"realm://dsts.core.azure-test.net/\",\"valueType\":\"http://www.w3.org/2001/XMLSchema#string\"}]}"

            val principal = new KafkaPrincipal(KafkaPrincipal.TOKEN_TYPE, tokenJsonString)
            val localHost = java.net.InetAddress.getLocalHost
            val session = Session(principal, localHost)
            val resource = Resource(Topic, "testTopic", PatternType.LITERAL)

            val authorizer: AzPubSubAclAuthorizer = EasyMock.partialMockBuilder(classOf[AzPubSubAclAuthorizer])
              .addMockedMethod("getAcls", classOf[Resource])
              .createMock()
            val acls = Set(Acl(new KafkaPrincipal("Role", "NotExistingClaim"), Allow, "*", All))
            EasyMock.expect(authorizer.getAcls(isA(classOf[Resource]))).andReturn(acls).anyTimes()
            suppress(MemberMatcher.methodsDeclaredIn(classOf[KafkaMetricsGroup]))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "newGauge"))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "markMetricsForAclAuthorizationRequest"))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "markMetricsForInvalidFromDateInToken"))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "markMetricsForInvalidToDateInToken"))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "markMetricsForUnauthorizedToken"))
            suppress(MemberMatcher.method(classOf[AzPubSubAclAuthorizer], "markMetricsForTokenIsAuthorized"))

            val logger: org.slf4j.Logger = EasyMock.mock(classOf[org.slf4j.Logger])

            EasyMock.expect(logger.info(isA(classOf[String]))).andVoid().anyTimes()
            EasyMock.expect(logger.debug(isA(classOf[String]))).andVoid().anyTimes()
            EasyMock.expect(logger.warn(isA(classOf[String]))).andVoid().anyTimes()

            val cache = new mutable.HashMap[String, Date]
            val validator = new mockPositiveTokenValidator

            Whitebox.setInternalState(authorizer, "cacheTokenLastValidatedTime", cache.asInstanceOf[Any])
            Whitebox.setInternalState(authorizer, "tokenAuthenticator", validator.asInstanceOf[Any])
            Whitebox.setInternalState(authorizer, "topicsWhiteListed", mutable.HashSet[String]("topic1", "testTopic").asInstanceOf[Any])

            EasyMock.replay(authorizer)

            assert(true == authorizer.authorize(session, All, resource))
            assert(cache.size == 0)
            EasyMock.verify(authorizer)
      }
}
