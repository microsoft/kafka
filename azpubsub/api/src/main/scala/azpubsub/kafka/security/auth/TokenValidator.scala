package azpubsub.kafka.security.auth

import java.util.Map
import azpubsub.kafka.security.authenticator.AzPubSubOAuthBearerToken

/**
  * Interface to validate base64 encoded token from Kafka client.
  */
trait TokenValidator {

  /**
    * Interface to pass in configuration settings.
    * @param javaConfigs configuration settings
    * @return void
    * @throws Exception
    */
  @throws[Exception]
  def configure(javaConfigs: Map[String, _]): Unit

  /**
    * @param base64TokenString a token encoded in base64
    * @return AzPubSubOAuthBearerToken
    * @throws TokenExpiredException
    * @throws InvalidTokenException
    */
  @throws[InvalidTokenException]
  @throws[AuthenticationFailedException]
  @throws[TokenExpiredException]
  @throws[NoClaimInTokenException]
  @throws[TokenValidationException]
  def validate(base64TokenString: String) : AzPubSubOAuthBearerToken

  /**
    * validate token from client, token is allowed to be exipired.
    * @param base64TokenString token encoded in base64
    * @return true - if token is valid or even the token is expired but it is not recalled;
    *         false - if token is invalid (siganture validation failed, etc.)
    * @throws InvalidTokenException
    */
  @throws[InvalidTokenException]
  @throws[AuthenticationFailedException]
  @throws[TokenExpiredException]
  @throws[NoClaimInTokenException]
  @throws[TokenValidationException]
  def validateWithTokenExpiredAllowed(base64TokenString: String) : Boolean
}
