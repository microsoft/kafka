package com.microsoft.azpubsub.security.authenticator;

import javax.security.sasl.SaslServer;

import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.security.auth.AuthenticationContext;
import org.apache.kafka.common.security.auth.KafkaPrincipal;
import org.apache.kafka.common.security.auth.SaslAuthenticationContext;
import org.apache.kafka.common.security.authenticator.DefaultKafkaPrincipalBuilder;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule;

import com.microsoft.azpubsub.security.auth.AzPubSubPrincipal;
import com.microsoft.azpubsub.security.oauthbearer.AzPubSubOAuthBearerToken;

public class AzPubSubPrincipalBuilder extends DefaultKafkaPrincipalBuilder {
    public AzPubSubPrincipalBuilder() {
        super(null, null);
    }

    @Override
    public KafkaPrincipal build(AuthenticationContext context) {
        if (context instanceof SaslAuthenticationContext) {
            SaslServer saslServer = ((SaslAuthenticationContext) context).server();
            if (!SaslConfigs.GSSAPI_MECHANISM.equals(saslServer.getMechanismName())) {
                AzPubSubOAuthBearerToken token = (AzPubSubOAuthBearerToken)saslServer.getNegotiatedProperty(OAuthBearerLoginModule.OAUTHBEARER_MECHANISM + ".token");
                return new AzPubSubPrincipal(
                            KafkaPrincipal.USER_TYPE,
                            token.principalName(),
                            token.value(),
                            token.lifetimeMs(),
                            token.startTimeMs(),
                            token.claims()
                        );
            }
        }

        return super.build(context);
    }
}
