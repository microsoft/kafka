import azpubsub.kafka.security.authenticator.AzPubSubPrincipal;
import azpubsub.kafka.security.authenticator.SaslAuthenticationContextValidator;
import azpubsub.kafka.security.authenticator.SslAuthenticationContextValidator;
import kafka.server.KafkaConfig;
import org.apache.kafka.common.errors.IllegalSaslStateException;
import org.apache.kafka.common.security.auth.KafkaPrincipal;
import org.apache.kafka.common.security.auth.SaslAuthenticationContext;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.kafka.common.security.auth.SslAuthenticationContext;
import org.apache.kafka.common.security.authenticator.AzPubSubPrincipalBuilder;
import org.apache.kafka.common.utils.Utils;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.api.support.membermodification.MemberModifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;

import javax.net.ssl.ExtendedSSLSession;
import javax.net.ssl.SSLSession;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslServer;
import java.net.InetAddress;
import java.util.Map;
import java.util.HashMap;


public class AzPubSubPrincipalBuilderTest {
    private final String AzPubSubSslAuthenticationValidatorClass = "azpubsub.ssl.authentication.validator.class";
    private final String AzPubSubSaslAuthenticationValidatorClass = "azpubsub.sasl.authentication.validator.class";
    private final String MockPositiveSslAuthenticationContextValidator = "mockPositiveSslAuthenticationContextValidator";
    private final String MockPositiveSaslAuthenticationValidator = "mockPositiveSaslAuthenticationContextValidator";

    private AzPubSubPrincipalBuilder azPubSubPrincipalBuilder = new AzPubSubPrincipalBuilder();

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Before
    public void setUp() {

    }

    @Test
    public void testConfigurePositive() {
        Map<String, Object> configs = new HashMap<>();

        configs.put(AzPubSubSslAuthenticationValidatorClass, MockPositiveSslAuthenticationContextValidator);
        configs.put(AzPubSubSaslAuthenticationValidatorClass, MockPositiveSaslAuthenticationValidator);
        MemberModifier.suppress(MemberMatcher.methodsDeclaredIn(Logger.class));

        azPubSubPrincipalBuilder.configure(configs);
    }

    @Test
    public void testConfigureSslContextValidatorClassNotExists() {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage("ClassNotFound");
        expectedEx.expectMessage("mockNonExistingSslAuthenticationContextValidator");

        Map<String, Object> configs = new HashMap<>();

        configs.put(AzPubSubSslAuthenticationValidatorClass, MockPositiveSslAuthenticationContextValidator);
        configs.put(AzPubSubSaslAuthenticationValidatorClass, MockPositiveSaslAuthenticationValidator);
        MemberModifier.suppress(MemberMatcher.methodsDeclaredIn(Logger.class));

        azPubSubPrincipalBuilder.configure(configs);
    }

    @Test
    public void testConfigureSaslContextValidatorClassNotExists() {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage("ClassNotFound");
        expectedEx.expectMessage("mockNonExistingPositiveSaslAuthenticationContextValidator");

        Map<String, Object> configs = new HashMap<>();

        configs.put(AzPubSubSslAuthenticationValidatorClass, MockPositiveSslAuthenticationContextValidator);
        configs.put(AzPubSubSaslAuthenticationValidatorClass, MockPositiveSaslAuthenticationValidator);
        MemberModifier.suppress(MemberMatcher.methodsDeclaredIn(Logger.class));

        azPubSubPrincipalBuilder.configure(configs);
    }

    @Test
    public void testValidateSslAuthenticationContextPositive() {
        Map<String, Object> configs = new HashMap<>();

        configs.put(AzPubSubSslAuthenticationValidatorClass, MockPositiveSslAuthenticationContextValidator);
        configs.put(AzPubSubSaslAuthenticationValidatorClass, MockPositiveSaslAuthenticationValidator);
        MemberModifier.suppress(MemberMatcher.methodsDeclaredIn(Logger.class));

        SSLSession sslSession = EasyMock.mock(SSLSession.class);
        InetAddress inetAddress =  InetAddress.getLoopbackAddress();

        azPubSubPrincipalBuilder.configure(configs);

        SslAuthenticationContext sslAuthenticationContext = new SslAuthenticationContext(sslSession, inetAddress);
        KafkaPrincipal kafkaPrincipal = azPubSubPrincipalBuilder.build(sslAuthenticationContext);
        assert(kafkaPrincipal.getName().equals("ANONYMOUS"));
        assert(KafkaPrincipal.ANONYMOUS.getPrincipalType().equals(kafkaPrincipal.getPrincipalType()));
        assert(KafkaPrincipal.ANONYMOUS.getName().equals(kafkaPrincipal.getName()));
    }

    @Test
    public void testValidateSaslAuthenticationContextPositive() {
        Map<String, Object> configs = new HashMap<>();

        configs.put(AzPubSubSslAuthenticationValidatorClass, MockPositiveSslAuthenticationContextValidator);
        configs.put(AzPubSubSaslAuthenticationValidatorClass, MockPositiveSaslAuthenticationValidator);
        MemberModifier.suppress(MemberMatcher.methodsDeclaredIn(Logger.class));

        SaslServer saslServer = EasyMock.mock(SaslServer.class);
        InetAddress inetAddress =  InetAddress.getLoopbackAddress();
        azPubSubPrincipalBuilder.configure(configs);

        SaslAuthenticationContext saslAuthenticationContext = new SaslAuthenticationContext(saslServer, SecurityProtocol.SASL_SSL, inetAddress);
        KafkaPrincipal kafkaPrincipal = azPubSubPrincipalBuilder.build(saslAuthenticationContext);
        assert(kafkaPrincipal.getPrincipalType().equals("Token"));
    }

    @Test
    public void testValidateSaslAuthenticationContextInvalidAuthentication() {
        expectedEx.expect(IllegalSaslStateException.class);
        expectedEx.expectMessage("failed to authenticate the current context");

        Map<String, Object> configs = new HashMap<>();

        configs.put(AzPubSubSslAuthenticationValidatorClass, MockPositiveSslAuthenticationContextValidator);
        configs.put(AzPubSubSaslAuthenticationValidatorClass, MockPositiveSaslAuthenticationValidator);
        MemberModifier.suppress(MemberMatcher.methodsDeclaredIn(Logger.class));

        SaslServer saslServer = EasyMock.mock(SaslServer.class);
        InetAddress inetAddress =  InetAddress.getLoopbackAddress();
        azPubSubPrincipalBuilder.configure(configs);

        SaslAuthenticationContext saslAuthenticationContext = new SaslAuthenticationContext(saslServer, SecurityProtocol.SASL_SSL, inetAddress);
        KafkaPrincipal kafkaPrincipal = azPubSubPrincipalBuilder.build(saslAuthenticationContext);
    }

    @Test
    public void testValidateSslAuthenticationContextWithInvalidAuthentication() {
        expectedEx.expect(IllegalStateException.class);
        expectedEx.expectMessage("Ssl Authentication Context Validator failed to validate the current SSL session");

        Map<String, Object> configs = new HashMap<>();

        configs.put(AzPubSubSslAuthenticationValidatorClass, MockPositiveSslAuthenticationContextValidator);
        configs.put(AzPubSubSaslAuthenticationValidatorClass, MockPositiveSaslAuthenticationValidator);
        MemberModifier.suppress(MemberMatcher.methodsDeclaredIn(Logger.class));

        SSLSession sslSession = EasyMock.mock(SSLSession.class);
        InetAddress inetAddress =  InetAddress.getLoopbackAddress();

        azPubSubPrincipalBuilder.configure(configs);

        SslAuthenticationContext sslAuthenticationContext = new SslAuthenticationContext(sslSession, inetAddress);
        KafkaPrincipal kafkaPrincipal = azPubSubPrincipalBuilder.build(sslAuthenticationContext);
    }
}
