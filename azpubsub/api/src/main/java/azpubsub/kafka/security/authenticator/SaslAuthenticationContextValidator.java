package azpubsub.kafka.security.authenticator;

import javax.security.sasl.SaslServer;

/**
 * Interface to authenticate Sasl Authentication Context
 */
public interface SaslAuthenticationContextValidator {
    /**
     * Interface to pass configuration settings to the validator
     * @param props configuration settings
     */
    void configure(java.util.Map<String, ?> props);

    /**
     * authenticate Sasl
     * @param saslServer saslServer is the plugin module used to authenticate the client.
     *                   For example, saslServer.evaluate(response) will be used to authenticate
     *                   client username/password, or tokens. Once evaluate() succeeds, authorization ID will
     *                   be stored in the context of saslServer, which will be used to construct client principal.
     * @return AzPubSubPrincipal
     */
    AzPubSubPrincipal authenticate(SaslServer saslServer);
}
