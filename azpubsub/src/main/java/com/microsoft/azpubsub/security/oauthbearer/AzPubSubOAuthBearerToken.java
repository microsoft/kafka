package com.microsoft.azpubsub.security.oauthbearer;

import java.util.HashSet;
import java.util.Set;

import org.apache.kafka.common.security.oauthbearer.OAuthBearerToken;

import com.microsoft.azpubsub.security.auth.Claim;

public class AzPubSubOAuthBearerToken implements OAuthBearerToken {
    private String value;
    private long lifetimeMs;
    private String principalName;
    private Long startTimeMs;
    private Set<Claim> claims;

    public AzPubSubOAuthBearerToken(String accessToken, long lifeTimeS, String principalName, Long startTimeMs) {
        super();
        this.value = accessToken;
        this.lifetimeMs = lifeTimeS;
        this.principalName= principalName;
        this.startTimeMs = startTimeMs;
        this.claims = new HashSet<Claim>();
    }

	@Override
	public String value() {
		return this.value;
	}

	@Override
	public Set<String> scope() {
		return null;
	}

	@Override
	public long lifetimeMs() {
		return this.lifetimeMs;
	}

	@Override
	public String principalName() {
		return this.principalName;
	}

	@Override
	public Long startTimeMs() {
		return this.startTimeMs;
	}

	public void addClaim(Claim claim) {
		claims.add(claim);
	}

	public Set<Claim> claims() {
		return claims;
	}
}
