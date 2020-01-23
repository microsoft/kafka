package com.microsoft.azpubsub.security.oauthbearer;

import org.apache.kafka.common.security.oauthbearer.OAuthBearerToken;

import com.microsoft.azpubsub.security.auth.AzPubSubConfig;

public interface OAuthAuthenticateValidator {
    public void configure(AzPubSubConfig config);

    public OAuthBearerToken introspectBearer(String accessToken);
}
