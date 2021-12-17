package com.microsoft.azpubsub.security

import com.yammer.metrics.core.Meter
import kafka.utils.Pool

import java.util.concurrent.TimeUnit
import scala.jdk.CollectionConverters._

class AuthenticatorMetrics(tags: scala.collection.Map[String, String]) extends AzPubSubSecurityMetrics {

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
        AuthenticatorStats.AuthenticatorServerSuccessPerSec -> MeterWrapper(AuthenticatorStats.AuthenticatorServerSuccessPerSec, "success"),
        AuthenticatorStats.AuthenticatorServerFailurePerSec -> MeterWrapper(AuthenticatorStats.AuthenticatorServerFailurePerSec, "failure"),
        AuthenticatorStats.AuthenticatorClientSuccessPerSec -> MeterWrapper(AuthenticatorStats.AuthenticatorClientSuccessPerSec, "success"),
        AuthenticatorStats.AuthenticatorClientFailurePerSec -> MeterWrapper(AuthenticatorStats.AuthenticatorClientFailurePerSec, "failure"),
        AuthenticatorStats.AuthenticatorClientTrustDisabledPerSec -> MeterWrapper(AuthenticatorStats.AuthenticatorClientTrustDisabledPerSec, "success"),
        AuthenticatorStats.AuthenticatorDsmsCertCacheSuccessPerSec -> MeterWrapper(AuthenticatorStats.AuthenticatorDsmsCertCacheSuccessPerSec, "success"),
        AuthenticatorStats.AuthenticatorDsmsCertCacheFailurePerSec -> MeterWrapper(AuthenticatorStats.AuthenticatorDsmsCertCacheFailurePerSec, "failure"),
    ).asJava)

    def serverSuccessRate = metricTypeMap.get(AuthenticatorStats.AuthenticatorServerSuccessPerSec).meter()

    def serverFailureRate = metricTypeMap.get(AuthenticatorStats.AuthenticatorServerFailurePerSec).meter()

    def clientSuccessRate = metricTypeMap.get(AuthenticatorStats.AuthenticatorClientSuccessPerSec).meter()

    def clientFailureRate = metricTypeMap.get(AuthenticatorStats.AuthenticatorClientFailurePerSec).meter()

    def clientTrustDisabledRate = metricTypeMap.get(AuthenticatorStats.AuthenticatorClientTrustDisabledPerSec).meter()

    def dsmsCertCacheSuccessRate = metricTypeMap.get(AuthenticatorStats.AuthenticatorDsmsCertCacheSuccessPerSec).meter()

    def dsmsCertCacheFailureRate = metricTypeMap.get(AuthenticatorStats.AuthenticatorDsmsCertCacheFailurePerSec).meter()
}

object AuthenticatorStats {
    val AuthenticatorServerSuccessPerSec = "AuthenticatorServerSuccessPerSec"
    val AuthenticatorServerFailurePerSec = "AuthenticatorServerFailurePerSec"
    val AuthenticatorClientSuccessPerSec = "AuthenticatorClientSuccessPerSec"
    val AuthenticatorClientFailurePerSec = "AuthenticatorClientFailurePerSec"
    val AuthenticatorClientTrustDisabledPerSec = "AuthenticatorClientTrustDisabledPerSec"
    val AuthenticatorDsmsCertCacheSuccessPerSec = "AuthenticatorDsmsCertCacheSuccessPerSec"
    val AuthenticatorDsmsCertCacheFailurePerSec = "AuthenticatorDsmsCertCacheFailurePerSec"
}

class AuthenticatorStats {
    private val stats = new Pool[String, AuthenticatorMetrics]

    def allStats(identity: String, authType: String): AuthenticatorMetrics = {
        val identityStr = identity.replaceAll(",", "_").replaceAll(":", "_").replaceAll("\\\\\\\\", "_").replaceAll("\\\\", "_").replaceAll("//", "_").replaceAll("/", "_")
        val tags: scala.collection.Map[String, String] = Map("auth-type" -> authType, "identity" -> identityStr)

        val key = authType + identityStr
        if (!stats.contains(key)) {
            stats.put(key, new AuthenticatorMetrics(tags))
        }

        stats.get(key)
    }
}
