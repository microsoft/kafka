package com.microsoft.azpubsub.security.auth

import scala.collection.JavaConverters.asScalaSetConverter

import org.apache.kafka.common.security.auth.KafkaPrincipal

import kafka.network.RequestChannel.Session
import kafka.security.auth.Operation
import kafka.security.auth.Resource
import kafka.security.auth.SimpleAclAuthorizer
import kafka.utils.Logging

/*
 * AzPubSub ACL Authorizer to handle the role
 */
class AzPubSubAclAuthorizer extends SimpleAclAuthorizer with Logging {
  override def authorize(session: Session, operation: Operation, resource: Resource): Boolean = {
    val sessionPrincipal = session.principal
    if (classOf[AzPubSubPrincipal] != sessionPrincipal.getClass)
      return super.authorize(session, operation, resource)

    val principal = sessionPrincipal.asInstanceOf[AzPubSubPrincipal]
    for (role <- principal.getRoles.asScala) {
      val claimPrincipal = new KafkaPrincipal(KafkaPrincipal.USER_TYPE, role)
      val claimSession = new Session(claimPrincipal, session.clientAddress)
      if (super.authorize(claimSession, operation, resource))
        return true
    }

    return false
  }
}
