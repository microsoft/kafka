package azpubsub.kafka.security.authenticator;

import java.util.ArrayList;
import java.util.List;

public class AzPubSubOAuthBearerToken {
    private Long validFromTicks = null;
    private Long validToTicks = null;
    private List<Claim> claims = null;
    private String originalBase64Token = null;

    /**
     * Constructor
     * @param vf valid from field of the original token, in ticks
     * @param vt valid to field of the original token, in ticks
     * @param token the original token in base64 string
     */
    public AzPubSubOAuthBearerToken(Long vf, Long vt, String token) {
        validFromTicks = vf;
        validToTicks = vt;
        claims = new ArrayList<>();
        originalBase64Token = token;
    }

    public void AddClaim(String ct,
                         String iss,
                         String oriIss,
                         String lbl,
                         String nct,
                         String rct,
                         String v,
                         String vt) {
        claims.add(new Claim(ct, iss, oriIss, lbl, nct, rct, v, vt));
    }

    public Long getValidFromTicks() {
        return validFromTicks;
    }

    public Long getValidToTicks() {
        return validToTicks;
    }
}
