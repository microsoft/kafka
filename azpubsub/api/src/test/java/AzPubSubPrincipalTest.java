import azpubsub.kafka.security.authenticator.AzPubSubPrincipal;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class AzPubSubPrincipalTest {
    @Test
    public void testAzPubSubPrincipal() {
        AzPubSubPrincipal azPubSubPrincipal = new AzPubSubPrincipal("testType123", "testName123");

        assertTrue("AzPubSubPrincipal is built successfully", azPubSubPrincipal.getPrincipalType().equals("testType123"));
        assertTrue("AzPubSubPrincipal is built successfully", azPubSubPrincipal.getPrincipalName().equals("testName123"));
    }
}
