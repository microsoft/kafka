package azpubsub.kafka.security.authenticator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AzPubSubOAuthBearerToken implements Comparable<AzPubSubOAuthBearerToken> {
    private String tokenId = null;
    private Long validFromTicks = null;
    private Long validToTicks = null;
    private List<Claim> claims = null;
    private String originalBase64Token = null;

    /**
     * Constructor
     * @param id token id, from the original token
     * @param vf valid from field of the original token, in ticks
     * @param vt valid to field of the original token, in ticks
     * @param token the original token in base64 string
     */
    public AzPubSubOAuthBearerToken(String id, Long vf, Long vt, String token) {
        tokenId = id;
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

    public String getTokenId() {
        return tokenId;
    }

    public Long getValidFromTicks() {
        return validFromTicks;
    }

    public Long getValidToTicks() {
        return validToTicks;
    }

    public int compareTo(AzPubSubOAuthBearerToken another) {
        if(validToTicks == another.validToTicks
                && validFromTicks == another.validFromTicks
                && claims != null && another.claims != null) {

            Comparator<Claim> comparator = new Comparator<Claim>() {
                @Override
                public int compare(Claim o1, Claim o2) {
                    if(null == o1 && null == o2) return 0;
                    if(null == o1) return -compare(o2, null);
                    return o2 == null ?  1 : o1.compareTo(o2);
                }
            };

            claims.sort(comparator);
            another.claims.sort(comparator);

            int i = 0;
            for(; i < claims.size()
                    && i < another.claims.size()
                    && 0 == claims.get(i).compareTo(another.claims.get(i));
                ++i) { }

             if(i == claims.size() && i == another.claims.size()) return 0;
        }
        return 1;
    }
}
