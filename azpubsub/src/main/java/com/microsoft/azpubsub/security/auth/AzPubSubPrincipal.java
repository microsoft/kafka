package com.microsoft.azpubsub.security.auth;

import java.util.Set;

import org.apache.kafka.common.security.auth.KafkaPrincipal;

public class AzPubSubPrincipal extends KafkaPrincipal {
    private String base64Token;
    private long validToTicks;
    private Long validFromTicks;
    private Set<Claim> claims;

	public AzPubSubPrincipal(String principalType, String name, String base64Token, long validToTicks, Long validFromTicks, Set<Claim> claims) {
		super(principalType, name);
		this.base64Token = base64Token;
		this.validToTicks = validToTicks;
		this.validFromTicks = validFromTicks;
		this.claims = claims;
	}

    public String getBase64Token() {
		return base64Token;
	}

    public Long getValidToTicks() {
		return validToTicks;
	}

    public Long getValidFromTicks() {
		return validFromTicks;
	}

    public Set<Claim> getClaims() {
		return claims;
	}
}
