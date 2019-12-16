package azpubsub.contextvalidator.kafka.security.auth;

import azpubsub.kafka.security.authenticator.AzPubSubPrincipal;
import azpubsub.kafka.security.authenticator.SaslAuthenticationContextValidator;

import javax.security.sasl.SaslServer;

public class mockNegativeSaslAuthenticationContextValidator implements SaslAuthenticationContextValidator {
   public void configure(java.util.Map<String, ?> configs) {

   }

   public AzPubSubPrincipal authenticate(SaslServer saslServer) {
       return null;
   }
}
