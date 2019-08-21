import java.util

import azpubsub.kafka.security.auth.TokenValidator

class mockPositiveTokenValidator extends TokenValidator{
  override def configure(javaConfigs: util.Map[String, _]): Unit = { }

  override def validate(base64TokenString: String): Boolean = true
}
