package kafka.security.auth;
import azpubsub.kafka.security.auth.InvalidTokenException;
import azpubsub.kafka.security.auth.NoClaimInTokenException;
import azpubsub.kafka.security.auth.AuthenticationFailedException;
import azpubsub.kafka.security.auth.TokenExpiredException;
import azpubsub.kafka.security.auth.TokenValidationException;
import azpubsub.kafka.security.auth.TokenValidator;
import azpubsub.kafka.security.authenticator.AzPubSubOAuthBearerToken;
import jdk.nashorn.internal.parser.Token;
import kafka.utils.Json;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerExtensionsValidatorCallback;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerToken;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerValidatorCallback;
import org.apache.kafka.common.security.oauthbearer.internals.unsecured.OAuthBearerIllegalTokenException;
import org.apache.kafka.common.security.oauthbearer.internals.unsecured.OAuthBearerValidationResult;
import org.apache.kafka.common.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import java.io.*;
import java.util.*;

import static java.lang.System.getProperty;

public class AzPubsubAuthenticateCallbackHandler implements AuthenticateCallbackHandler{
    private static final String TokenValidatorClassPathKey = "azpubsub.token.validator.class";

    private static final Logger log = LoggerFactory.getLogger(AzPubsubAuthenticateCallbackHandler.class);
    private boolean configured = false;
    private Map<String, String> moduleOptions = null;
    private TokenValidator tokenValidator = null;

    @SuppressWarnings("unchecked")
    @Override
    public void configure(Map<String, ?> configs,
                          String saslMechanism,
                          List<AppConfigurationEntry> jaasConfigEntries) {
        String azpubsubPropertiesFilePath = System.getProperty("azpubsub.properties");
            try (InputStream inputStream = new FileInputStream(new File(azpubsubPropertiesFilePath))) {
                Properties properties = new Properties();

                properties.load(inputStream);
                for (Map.Entry<String, ?> e: configs.entrySet()
                     ) {
                    if(null != e && null != e.getKey() && null != e.getValue()) {
                        properties.putIfAbsent(e.getKey(), e.getValue());
                    }
                }

                @SuppressWarnings({ "unchecked", "rawtypes" })
                Map<String, ?> allConfigs = new HashMap(properties);

                if (!OAuthBearerLoginModule.OAUTHBEARER_MECHANISM.equals(saslMechanism))
                    throw new IllegalArgumentException(String.format("Unexpected SASL mechanism: %s", saslMechanism));

                if(!allConfigs.containsKey(TokenValidatorClassPathKey)) {
                    throw new IllegalArgumentException(String.format("No token validator class is set via %s", TokenValidatorClassPathKey));
                }

                String validatorClassName = configs.get(TokenValidatorClassPathKey).toString();

                try {
                    tokenValidator = Utils.newInstance(validatorClassName, TokenValidator.class);
                    tokenValidator.configure(allConfigs);
                }
                catch (ClassNotFoundException ex) {
                    throw new IllegalArgumentException(String.format("Class %s configured by %s is not found! Error: %s", validatorClassName, TokenValidatorClassPathKey, ex.getMessage() ));
                }
                catch (java.lang.Exception ex) {
                    throw new RuntimeException(String.format("Exception happened. Error: {}", ex.getMessage()), ex.getCause());
                }

                configured = true;
            }
            catch (IOException ex) {

            }

    }

    public boolean configured() {
        return configured;
    }

    public void close() {
        //empty
    }

    @Override
    public void handle(javax.security.auth.callback.Callback[] callbacks)
            throws java.io.IOException, UnsupportedCallbackException {
        try {
            if (!configured())
                throw new IllegalStateException("Callback handler not configured");
            for (Callback callback : callbacks) {
                if (callback instanceof OAuthBearerValidatorCallback) {
                    OAuthBearerValidatorCallback validationCallback = (OAuthBearerValidatorCallback) callback;
                    try {
                        handleCallback(validationCallback);
                    } catch (OAuthBearerIllegalTokenException e) {
                        OAuthBearerValidationResult failureReason = e.reason();
                        String failureScope = failureReason.failureScope();
                        validationCallback.error(failureScope != null ? "insufficient_scope" : "invalid_token",
                                failureScope, failureReason.failureOpenIdConfig());
                    }
                } else if (callback instanceof OAuthBearerExtensionsValidatorCallback) {
                    OAuthBearerExtensionsValidatorCallback extensionsCallback = (OAuthBearerExtensionsValidatorCallback) callback;
                    extensionsCallback.inputExtensions().map().forEach((extensionName, v) -> extensionsCallback.valid(extensionName));
                } else
                    throw new UnsupportedCallbackException(callback);
            }
        }
        catch (Exception ex) {

        }
    }

    private void handleCallback(OAuthBearerValidatorCallback callback) {
        String tokenValue = callback.tokenValue();

        if (tokenValue == null)
            throw new IllegalArgumentException("Callback missing required token value");

        try {
            AzPubSubOAuthBearerToken token = tokenValidator.validate(tokenValue);

            callback.token(new OAuthBearerToken() {
                @Override
                public String value() {
                    return tokenValue;
                }

                @Override
                public Set<String> scope() {
                    return null;
                }

                @Override
                public long lifetimeMs() {
                    return token.getValidToTicks();
                }

                @Override
                public String principalName() {
                    return Json.encodeAsString(token);
                }

                @Override
                public Long startTimeMs() {
                    return token.getValidFromTicks();
                }
            });
        }
        catch (InvalidTokenException ex) {
            log.error(String.format("Token is invalid, validation failed: %s", tokenValue));
            throw new IllegalArgumentException(String.format("The received token is invalid. Error: %s", ex.getMessage()));
        }
        catch (TokenExpiredException ex) {
            log.error(String.format("Token is already expired: %s", tokenValue));
            throw new IllegalArgumentException(String.format("The received token is expired. Error: %s", ex.getMessage()));
        }
        catch (AuthenticationFailedException ex) {
            log.error(String.format("Token is already expired: %s", tokenValue));
            throw new IllegalArgumentException(String.format("The received token is expired. Error: %s", ex.getMessage()));
        }
        catch (NoClaimInTokenException ex) {
            log.error(String.format("Validation of the token is good, but there's no claim in the token. Token: %s", tokenValue));
            throw new IllegalArgumentException(String.format("Validation of the token is good, but there's no claim in the token. Error: %s", ex.getMessage()));
        }
        catch (TokenValidationException ex) {
            log.error(String.format("There's a validation exception happened when validating the token. Token: %s", tokenValue));
            throw new IllegalArgumentException(String.format("There's a validation exception happened when validating the token. Error: %s", ex.getMessage()));
        }
    }
}
