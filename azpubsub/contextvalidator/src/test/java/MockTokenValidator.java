package azpubsub.contextvalidator.kafka.security.test;

import azpubsub.kafka.security.auth.*;
import azpubsub.kafka.security.authenticator.AzPubSubOAuthBearerToken;

import java.util.Map;

public class MockTokenValidator implements TokenValidator {
    @Override
    public void configure(Map<String, ?> javaConfigs) throws Exception {

    }

    @Override
    public AzPubSubOAuthBearerToken validate(String base64TokenString) throws TokenValidationException, TokenExpiredException, InvalidTokenException, NoClaimInTokenException {
        return new AzPubSubOAuthBearerToken();
    }

    @Override
    public boolean validateWithTokenExpiredAllowed(String base64TokenString) throws TokenValidationException, TokenExpiredException, InvalidTokenException, NoClaimInTokenException {
        return true;
    }
}
