package com.microsoft.kafka.security.auth

import kafka.network.RequestChannel.Session
import kafka.security.auth.SimpleAclAuthorizer
import kafka.security.auth.Resource
import kafka.security.auth.Operation
import kafka.utils.Logging

class AzPubSubAclAuthorizer extends SimpleAclAuthorizer with Logging {
  override def configure(javaConfigs: util.Map[String, _]) {
    super.configure(javaConfigs)
  }

  override def authorize(session: Session, operation: Operation, resource: Resource): Boolean = {
    val sessionPrincipal = session.principal
    if (classOf[AzPubSubPrincipal] != sessionPrincipal.getClass)
      return super.authorize(session, operation, resource)

    for (i <- 0 until token.getClaims.size()) {
      val claim = token.getClaims.get(i)
      val principal = new KafkaPrincipal(KafkaPrincipal.USER_TYPE, claim.getValue)
      var newSession = new Session(principal, session.clientAddress)
      if (super.authorize(newSession, operation, resource))
        return true
    }

    return false
  }
}
