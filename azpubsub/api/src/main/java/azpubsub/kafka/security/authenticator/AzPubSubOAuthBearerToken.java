package azpubsub.kafka.security.authenticator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AzPubSubOAuthBearerToken implements Comparable<AzPubSubOAuthBearerToken> {
    @JsonProperty("tokenId")
    private String tokenId = null;

    @JsonProperty("validFromTicks")
    private Long validFromTicks = null;

    @JsonProperty("validToTicks")
    private Long validToTicks = null;

    @JsonProperty("claims")
    private List<Claim> claims = null;

    @JsonProperty("originalBase64Token")
    private String originalBase64Token = null;

    public AzPubSubOAuthBearerToken (){}

    @JsonCreator
    public AzPubSubOAuthBearerToken(@JsonProperty("tokenId") String id,
                                    @JsonProperty("validFromTicks") Long vf,
                                    @JsonProperty("validToTicks") Long vt,
                                    @JsonProperty("originalBase64Token") String token,
                                    @JsonProperty("claims") List<Claim> cls) {
        tokenId = id;
        validFromTicks = vf;
        validToTicks = vt;
        originalBase64Token = token;
        claims = new ArrayList<>();
        for (Claim c : cls
        ) {
            claims.add(c);
        }
    }
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

    public void AddClaim(Claim cl) {
        claims.add(cl);
    }

    @JsonProperty("tokenId")
    public String getTokenId() {
        return tokenId;
    }

    @JsonProperty("tokenId")
    public void setTokenId(String id) { tokenId = id; }

    @JsonProperty("validFromTicks")
    public Long getValidFromTicks() {
        return validFromTicks;
    }

    @JsonProperty("validFromTicks")
    public void setValidFromTicks(Long vf) {validFromTicks = vf;}

    @JsonProperty("validToTicks")
    public Long getValidToTicks() {
        return validToTicks;
    }

    @JsonProperty("validToTicks")
    public void setValidToTicks(Long vt) { validToTicks = vt;}

    @JsonProperty("claims")
    public List<Claim> getClaims() {
        return claims;
    }

    @JsonProperty("claims")
    public void setClaims(List<Claim> cls) {
        for (Claim cl: cls ) {
           claims.add(cl);
        }
    }

    @JsonProperty("originalBase64Token")
    public String getOriginalBase64Token() {
        return originalBase64Token;
    }

    @JsonProperty("originalBase64Token")
    public void setOriginalBase64Token(String token) {
        originalBase64Token = token;
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
