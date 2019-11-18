import azpubsub.kafka.security.auth.{InvalidTokenException, TokenValidator}
import java.util

import azpubsub.kafka.security.authenticator.AzPubSubOAuthBearerToken

class mockNegativeTokenValidator extends TokenValidator{
  override def configure(javaConfigs: util.Map[String, _]): Unit = { }

  override def validate(base64TokenString: String): AzPubSubOAuthBearerToken = {
    null
  }

  def validateWithTokenExpiredAllowed(base64TokenString: String) : Boolean = {
    false
  }
}
