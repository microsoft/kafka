package azpubsub.contextvalidator.kafka.security.auth;

import azpubsub.contextvalidator.kafka.security.auth.AzPubsubAuthenticateCallbackHandler;
import azpubsub.contextvalidator.kafka.security.auth.ConfigUtils;
import azpubsub.kafka.security.auth.*;
import azpubsub.kafka.security.authenticator.AzPubSubOAuthBearerToken;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerValidatorCallback;
import org.apache.kafka.common.utils.Utils;
import org.easymock.EasyMock;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


@RunWith(PowerMockRunner.class)
@PrepareForTest({ConfigUtils.class, Utils.class})
public class AzPubsubAuthenticateCallbackHandlerTest {

    public class MockPositiveTokenValidator implements TokenValidator {
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

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void configureTestConfigUtilsFailedToLoadConfigs() {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage("Failed to read AzPubSub properties or failed to merge it to global Kafka configs");

        Map<String, String> configs = new HashMap<>();
        configs.put("key1", "value1");
        configs.put("key2", "value2");

        PowerMock.mockStatic(ConfigUtils.class);
        EasyMock.expect(ConfigUtils.loadAzPubsubConfigAndMergeGlobalConfig(configs)).andReturn(null);
        PowerMock.replay(ConfigUtils.class);

        AzPubsubAuthenticateCallbackHandler handler = new AzPubsubAuthenticateCallbackHandler();
        handler.configure(configs, "OAUTHBEARER", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void configureTestMechanismNoExisting() {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage("Failed to read AzPubSub properties or failed to merge it to global Kafka configs");

        Map<String, String> configs = new HashMap<>();
        configs.put("key1", "value1");
        configs.put("key2", "value2");

        Map<String, String> allConfigs = new HashMap<>(configs);
        allConfigs.put("key3", "value3");

        PowerMock.mockStatic(ConfigUtils.class);

        EasyMock.expect(ConfigUtils.loadAzPubsubConfigAndMergeGlobalConfig(configs)).andReturn(allConfigs);
        PowerMock.replay(ConfigUtils.class);

        AzPubsubAuthenticateCallbackHandler handler = new AzPubsubAuthenticateCallbackHandler();
        handler.configure(configs, "FAKEMECHANISM", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void configureTestValidatorClassNotConfigured() {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage("No token validator class is set via ");

        Map<String, String> configs = new HashMap<>();
        configs.put("key1", "value1");
        configs.put("key2", "value2");

        Map<String, String> allConfigs = new HashMap<>(configs);
        allConfigs.put("key3", "value3");

        PowerMock.mockStatic(ConfigUtils.class);

        EasyMock.expect(ConfigUtils.loadAzPubsubConfigAndMergeGlobalConfig(configs)).andReturn(allConfigs).anyTimes();
        PowerMock.replay(ConfigUtils.class);

        AzPubsubAuthenticateCallbackHandler handler = new AzPubsubAuthenticateCallbackHandler();
        handler.configure(configs, "OAUTHBEARER", null);
    }

    @Test
    public void configurePositive() {
        Map<String, String> configs = new HashMap<>();
        configs.put("key1", "value1");
        configs.put("key2", "value2");

        Map<String, String> allConfigs = new HashMap<>(configs);
        allConfigs.put(ConfigUtils.AzpubsubTokenValidatorClassPathKey, "azpubsub.contextvalidator.kafka.security.test.MockTokenValidator");

        PowerMock.mockStatic(ConfigUtils.class);

        EasyMock.expect(ConfigUtils.loadAzPubsubConfigAndMergeGlobalConfig(configs)).andReturn(allConfigs).anyTimes();
        PowerMock.replay(ConfigUtils.class);

        AzPubsubAuthenticateCallbackHandler handler = new AzPubsubAuthenticateCallbackHandler();
        handler.configure(configs, "OAUTHBEARER", null);

        assert( handler.configured() );
    }

    @Test
    public void configureTestValidatorClassNotFound() {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage("is not found!");
        Map<String, String> configs = new HashMap<>();
        configs.put("key1", "value1");
        configs.put("key2", "value2");

        Map<String, String> allConfigs = new HashMap<>(configs);
        allConfigs.put(ConfigUtils.AzpubsubTokenValidatorClassPathKey, "azpubsub.contextvalidator.kafka.security.test.NotExistingTokenValidator");

        PowerMock.mockStatic(ConfigUtils.class);

        EasyMock.expect(ConfigUtils.loadAzPubsubConfigAndMergeGlobalConfig(configs)).andReturn(allConfigs).anyTimes();
        PowerMock.replay(ConfigUtils.class);

        AzPubsubAuthenticateCallbackHandler handler = new AzPubsubAuthenticateCallbackHandler();
        handler.configure(configs, "OAUTHBEARER", null);
    }

    @Test
    public void configureTestHandleCallbackPositive() {
        Map<String, String> configs = new HashMap<>();
        configs.put("key1", "value1");
        configs.put("key2", "value2");

        Map<String, String> allConfigs = new HashMap<>(configs);
        allConfigs.put(ConfigUtils.AzpubsubTokenValidatorClassPathKey, "azpubsub.contextvalidator.kafka.security.test.MockTokenValidator");

        PowerMock.mockStatic(ConfigUtils.class);

        EasyMock.expect(ConfigUtils.loadAzPubsubConfigAndMergeGlobalConfig(configs)).andReturn(allConfigs).anyTimes();
        PowerMock.replay(ConfigUtils.class);

        PowerMock.mockStatic(Utils.class);

        try {
            TokenValidator validator = EasyMock.createMock(MockPositiveTokenValidator.class);

            EasyMock.expect(Utils.newInstance("azpubsub.contextvalidator.kafka.security.test.MockTokenValidator", TokenValidator.class)).andReturn(validator).once();
            PowerMock.replay(Utils.class);

            AzPubsubAuthenticateCallbackHandler handler = new AzPubsubAuthenticateCallbackHandler();
            handler.configure(configs, "OAUTHBEARER", null);

            assert (handler.configured());

            OAuthBearerValidatorCallback callback = new OAuthBearerValidatorCallback("AFakeToken");

            handler.handle(new Callback[]{callback});
        }
        catch(UnsupportedCallbackException ex) {

        }
        catch (ClassNotFoundException ex) {

        }
        catch(IOException ex) {

        }
    }
}
