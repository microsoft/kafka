import azpubsub.kafka.security.auth.TokenValidator
import java.util

class mockNegativeTokenValidator extends TokenValidator{
  override def configure(javaConfigs: util.Map[String, _]): Unit = { }

  override def validate(base64TokenString: String): Boolean = false

}
