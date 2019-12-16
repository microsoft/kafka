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

    /**
     * Constructor
     * @param id token id, it can be retrieved from the security token.
     * @param validateFrom when the token begins to be valid, UTC datetime in ticks
     * @param validateTo when the token is expired, UTC datetime in ticks
     * @param base64TokenString the original security token, encoded in base64
     * @param claimList claims in the security token
     */
    @JsonCreator
    public AzPubSubOAuthBearerToken(@JsonProperty("tokenId") String id,
                                    @JsonProperty("validFromTicks") Long validateFrom,
                                    @JsonProperty("validToTicks") Long validateTo,
                                    @JsonProperty("originalBase64Token") String base64TokenString,
                                    @JsonProperty("claims") List<Claim> claimList) {
        tokenId = id;
        validFromTicks = validateFrom;
        validToTicks = validateTo;
        originalBase64Token = base64TokenString;
        claims = new ArrayList<>();
        for (Claim c : claimList
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

    /**
     * Adding a new claim to the OAuth Bearer token
     * @param claimType Claim type
     * @param issuer Issuer of the token
     * @param originalIssuer Original issuer of the token
     * @param label Lable of the claim
     * @param nameClaimType Type of name claim
     * @param roleClaimType Type of role claim
     * @param value value of the claim
     * @param valueType type of the claim value
     */
    public void AddClaim(String claimType,
                         String issuer,
                         String originalIssuer,
                         String label,
                         String nameClaimType,
                         String roleClaimType,
                         String value,
                         String valueType) {
        claims.add(new Claim( claimType, issuer, originalIssuer, label, nameClaimType, roleClaimType, value, valueType));
    }

    /**
     *  Adding a claim to the OAuth Bearer token
     * @param claim
     */
    public void AddClaim(Claim claim) {
        claims.add(claim);
    }

    /**
     * Get token Id. The id is the original id of the security token
     * @return
     */
    @JsonProperty("tokenId")
    public String getTokenId() {
        return tokenId;
    }

    /**
     * Set id of the token. The id is the original id of the security token
     * @param id
     */
    @JsonProperty("tokenId")
    public void setTokenId(String id) { tokenId = id; }

    /**
     * Get datetime when the token begins to be valid, in ticks number, UTC.
     * @return
     */
    @JsonProperty("validFromTicks")
    public Long getValidFromTicks() {
        return validFromTicks;
    }

    /**
     * Set datetime when the token begins to be valid, in ticks number, UTC.
     * @param validFrom
     */
    @JsonProperty("validFromTicks")
    public void setValidFromTicks(Long validFrom) {validFromTicks = validFrom;}

    /**
     * Get datetime when the token is expired, in ticks number, UTC.
     * @return
     */
    @JsonProperty("validToTicks")
    public Long getValidToTicks() {
        return validToTicks;
    }

    /**
     * Set datetime when the token is expired, in ticks number, UTC.
     * @param validTo
     */
    @JsonProperty("validToTicks")
    public void setValidToTicks(Long validTo) { validToTicks = validTo;}

    /**
     * Get claims of the OAuth Bearer token
     * @return
     */
    @JsonProperty("claims")
    public List<Claim> getClaims() {
        return claims;
    }

    /**
     * Add claim list to the OAuth Bearer Token
     * @param claimList
     */
    @JsonProperty("claims")
    public void setClaims(List<Claim> claimList) {
        for (Claim cl: claimList) {
           claims.add(cl);
        }
    }

    /**
     * Get the original security token, encoded in Base64
     * @return
     */
    @JsonProperty("originalBase64Token")
    public String getOriginalBase64Token() {
        return originalBase64Token;
    }

    /**
     * Set the original security token to the OAuth Bearer token, encoded in Base64
     * @param token
     */
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
