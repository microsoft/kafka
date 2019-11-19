import java.util

import azpubsub.kafka.security.auth.TokenValidator
import azpubsub.kafka.security.authenticator.AzPubSubOAuthBearerToken

class mockPositiveTokenValidator extends TokenValidator{
  override def configure(javaConfigs: util.Map[String, _]): Unit = { }

  override def validate(base64TokenString: String): AzPubSubOAuthBearerToken = {
    val token = new AzPubSubOAuthBearerToken("testId", 0L, 10L, "FAKETOKEN")
    token.AddClaim("", "", "", "", "", "", "", "")
    token
  }

  override def validateWithTokenExpiredAllowed(base64TokenString: String): Boolean = true
}
