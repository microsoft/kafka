package com.microsoft.azpubsub.security.oauthbearer;

import org.apache.kafka.common.security.oauthbearer.OAuthBearerToken;

public interface OAuthAuthenticateValidator {
	public OAuthBearerToken introspectBearer(String accessToken);
}
