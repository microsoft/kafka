import azpubsub.kafka.security.authenticator.AzPubSubPrincipal;
import azpubsub.kafka.security.authenticator.SslAuthenticationContextValidator;

import javax.net.ssl.SSLSession;

public class mockPositiveSslAuthenticationContextValidator implements SslAuthenticationContextValidator {

    public void configure(java.util.Map<String, ?> configs) {

    }

    public AzPubSubPrincipal authenticate(SSLSession sslSession) {
        return new AzPubSubPrincipal("User", "ANONYMOUS");
    }
}
