package com.microsoft.azpubsub.security.auth

import java.net.InetAddress
import java.util
import java.util.Timer
import java.util.concurrent._

import com.yammer.metrics.core.{Meter, MetricName}
import kafka.metrics.KafkaMetricsGroup
import kafka.security.authorizer.AclAuthorizer
import kafka.utils.Logging
import org.apache.kafka.common.security.auth.{KafkaPrincipal, SecurityProtocol}
import org.apache.kafka.server.authorizer.{Action, AuthorizableRequestContext, AuthorizationResult}

import scala.collection.JavaConverters.asScalaSetConverter
import scala.collection.mutable

/*
 * AzPubSub ACL Authorizer to handle the certificate & role based principal type
 */

object AzPubSubAclAuthorizerV2 {
  val configThrottlingLevel = "azpubsub.qps.throttling.level"
  val configTopicThrottlingQps = "azpubsub.topic.max.qps"
  val configClientidTopicThrottlingQps = "azpubsub.clientid.topic.max.qps"
  val configMetterSuccessRatePerSec = "AuthorizerSuccessPerSec"
  val configMetterFailureRatePerSec = "AuthorizerFailurePerSec"
  val configTimerTaskExecutionInterval = "azpubsub.timer.task.execution.interval.in.ms"
}


class AzPubSubAclAuthorizerV2 extends AclAuthorizer with Logging with KafkaMetricsGroup {
  override def metricName(name: String, metricTags: scala.collection.Map[String, String]): MetricName = {
    explicitMetricName("azpubsub.security", "AuthorizerMetrics", name, metricTags)
  }

  override def configure(javaConfigs: util.Map[String, _]): Unit = {
    super.configure(javaConfigs)
    config = AzPubSubConfig.fromProps(javaConfigs)
    throttlingLevel = config.getInt(AzPubSubAclAuthorizerV2.configThrottlingLevel)
    throttlingTopicQps = config.getInt(AzPubSubAclAuthorizerV2.configTopicThrottlingQps)
    throttlingClientIdTopicQps = config.getInt(AzPubSubAclAuthorizerV2.configTopicThrottlingQps)
    timerTaskExecutionInterval = config.getLong(AzPubSubAclAuthorizerV2.configTimerTaskExecutionInterval)
    topicThreadCounterTimerTask  = new ThreadCounterTimerTask(timerTaskExecutionInterval, throttlingLevel)
    trigger.scheduleAtFixedRate(topicThreadCounterTimerTask, 2, timerTaskExecutionInterval)
    topicThreadCounterTimerTask.setIoThreadId(Thread.currentThread().getId)
  }

  private var config: AzPubSubConfig = null
  private var throttlingLevel: Int = 0
  private var throttlingTopicQps: Int = 0
  private var throttlingClientIdTopicQps: Int = 0
  private var timerTaskExecutionInterval: Long = 0
  private val successRate: Meter = newMeter(AzPubSubAclAuthorizerV2.configMetterSuccessRatePerSec, "success", TimeUnit.SECONDS)
  private val failureRate: Meter = newMeter(AzPubSubAclAuthorizerV2.configMetterFailureRatePerSec, "failure", TimeUnit.SECONDS)
  private var topicThreadCounterTimerTask : ThreadCounterTimerTask = null
  private val trigger: Timer = new Timer(true)

  var ints = new mutable.HashMap[String, mutable.TreeSet[Long]]

  override def authorize(requestContext: AuthorizableRequestContext, actions: util.List[Action]): util.List[AuthorizationResult] = {
    if(throttlingLevel > 0) {
      actions.forEach( a => if( a.resourcePattern().resourceType() == org.apache.kafka.common.resource.ResourceType.TOPIC) {
        topicThreadCounterTimerTask.setTopicName(a.resourcePattern.name())
        topicThreadCounterTimerTask.setClientId(requestContext.clientId())

        val key = makeKey(a.resourcePattern().name(), requestContext.clientId())
        if(!ints.contains(key)) ints.put(key, new mutable.TreeSet[Long]())

        val threadCount = Math.max(topicThreadCounterTimerTask.getThreadCount, 1)

        var count = 1
        while (throttlingLevel == 1 && ints.get(key).size * threadCount > throttlingTopicQps || throttlingLevel == 2 && ints.get(key).size * threadCount > throttlingClientIdTopicQps) {
          val pivot = ints.get(key).get.minBy(x => x > System.currentTimeMillis - 1000)
          val (_, after) = ints.get(key).get.partition(x => x > pivot)
          ints.put(key, after)
          Thread.sleep(count)
          count *= 2
        }

        ints.get(key).get += System.currentTimeMillis
      } )
    }

    var res : util.List[AuthorizationResult] = null

    if (requestContext.principal().getClass == classOf[AzPubSubPrincipal]) {
      val tmpPrincipal = requestContext.principal().asInstanceOf[AzPubSubPrincipal]
      for(role <- tmpPrincipal.getRoles.asScala) {
        val context : AuthorizableRequestContext =  new AuthorizableRequestContext {
          override def listenerName(): String = requestContext.listenerName()

          override def securityProtocol(): SecurityProtocol = requestContext.securityProtocol()

          override def principal(): KafkaPrincipal = {
            new KafkaPrincipal(tmpPrincipal.getPrincipalType, role)
          }

          override def clientAddress(): InetAddress = requestContext.clientAddress()

          override def requestType(): Int = requestContext.requestType()

          override def requestVersion(): Int = requestContext.requestVersion()

          override def clientId(): String = requestContext.clientId()

          override def correlationId(): Int = requestContext.correlationId()
        }
        res = super.authorize(context, actions)
        if (res.contains(AuthorizationResult.ALLOWED) ) {
          successRate.mark()
          res
        }
      }
      failureRate.mark()
      return res
    }
    res = super.authorize(requestContext, actions)
    if(null != res && res.contains(AuthorizationResult.ALLOWED)) {
      successRate.mark()
    }
    else {
      failureRate.mark()
    }
    return res
  }

  private def makeKey(topic: String, clientId: String) : String = {
    throttlingLevel match {
      case 1 => String.format("ClientId:%s|Topic:%s", topic, clientId)
      case 2 => String.format("ClientId:%s", clientId)
      case _ => String.format("Topic:%s", topic)
    }
  }
}
