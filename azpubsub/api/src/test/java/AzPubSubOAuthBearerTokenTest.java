import azpubsub.kafka.security.authenticator.AzPubSubOAuthBearerToken;
import kafka.utils.Json;
import org.junit.Test;
import scala.reflect.ClassTag;

import static org.junit.Assert.assertTrue;

public class AzPubSubOAuthBearerTokenTest {
    @Test
    public void testConstructor() {
        String id = "_23eb1c5c-4cba-4e9e-a16d-44c342c9de75";
        Long vf = 324244L;
        Long vt = 553243L;
        String originalToken = "TZkLTQ0YzM0MmM5ZGU3NSIgSXNzdWVJbnN0YW50PSIyMDE5LTExLTIwVDIyOjE1OjIxLjA0MVoiIFZlcnNpb249IjIuMCIgeG1sbnM9InVybjpvYX";
        AzPubSubOAuthBearerToken token = new AzPubSubOAuthBearerToken(id, vf, vt, originalToken);
        token.AddClaim("ct1", "iss1", "oriIss1", "lbl1", "nct1", "rct1", "v1", "vt1");

        assertTrue(id.equals(token.getTokenId()));
        assertTrue(token.getValidFromTicks() == vf);
        assertTrue(token.getValidToTicks() == vt);
        assertTrue(token.getClaims().size() == 1);
    }

    @Test
    public void testJsonDeserialization() {
        String jsonString = "{\"tokenId\":\"_ddebd0c4-1965-42da-9ce5-4a5e36c5e5aa\",\"validFromTicks\":637099144420490000,\"validToTicks\":637099162420490000,\"originalBase64Token\":\"PEFzc2VydGlvbiBJRD0iX2\"}";

    }

}
