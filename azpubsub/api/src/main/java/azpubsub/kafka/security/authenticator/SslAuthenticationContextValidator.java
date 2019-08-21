package azpubsub.kafka.security.authenticator;

import javax.net.ssl.SSLSession;
import java.util.Map;

/**
 * Interface to validate Ssl authentication context, including client certificates.
 */
public interface SslAuthenticationContextValidator {
    /**
     * Interface to pass configuration settings onto the validator
     * @param props
     */
    void configure(Map<String, ?> props);

    /**
     * authenticate the SSL session, including client certificates.
     * @param sslSession SSL Session context
     * @return AzPubSubPrincipal
     */
    AzPubSubPrincipal authenticate(SSLSession sslSession);
}