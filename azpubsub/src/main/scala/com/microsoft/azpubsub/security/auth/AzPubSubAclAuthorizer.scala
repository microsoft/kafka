package com.microsoft.azpubsub.security.auth

import collection.JavaConverters._
import java.util

import kafka.network.RequestChannel.Session
import kafka.security.auth.SimpleAclAuthorizer
import kafka.security.auth.Resource
import kafka.security.auth.Operation
import kafka.utils.Logging

import org.apache.kafka.common.security.auth.KafkaPrincipal

class AzPubSubAclAuthorizer extends SimpleAclAuthorizer with Logging {
  override def configure(javaConfigs: util.Map[String, _]) {
    super.configure(javaConfigs)
  }

  override def authorize(session: Session, operation: Operation, resource: Resource): Boolean = {
    val sessionPrincipal = session.principal
    if (classOf[AzPubSubPrincipal] != sessionPrincipal.getClass)
      return super.authorize(session, operation, resource)

    val principal = sessionPrincipal.asInstanceOf[AzPubSubPrincipal]
    for (claim <- principal.getClaims.asScala) {
      val claimPrincipal = new KafkaPrincipal(KafkaPrincipal.USER_TYPE, claim.getValue)
      val claimSession = new Session(claimPrincipal, session.clientAddress)
      if (super.authorize(claimSession, operation, resource))
        return true
    }

    return false
  }
}
