package com.microsoft.kafka.security.auth;

import java.util.List;

import org.apache.kafka.common.security.auth.KafkaPrincipal;

public class AzPubSubPrincipal extends KafkaPrincipal {
	private String tokenId;
    private Long validFromTicks;
    private Long validToTicks;
    private List<Claim> claims;
    private String originalBase64Token;

	public AzPubSubPrincipal(String principalType, String name) {
		super(principalType, name);
	}

    public String getTokenId() {
		return tokenId;
	}

    public void setTokenId(String tokenId) {
		this.tokenId = tokenId;
	}

    public Long getValidFromTicks() {
		return validFromTicks;
	}

    public Long getValidToTicks() {
		return validToTicks;
	}

    public List<Claim> getClaims() {
		return claims;
	}

    public String getOriginalBase64Token() {
		return originalBase64Token;
	}
}
