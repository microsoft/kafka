package com.microsoft.azpubsub.security.authenticator;

import java.util.HashSet;
import java.util.Set;

/*
 * AzPubSub Certificate Identity model
 */
public class CertificateIdentity {
    private String principalName;
    private Set<String> scopes;

    public CertificateIdentity(String principalName) {
        this.principalName = principalName;
        this.scopes = new HashSet<String>();
    }

    public String principalName() {
        return this.principalName;
    }

    public Set<String> scope() {
        return this.scopes;
    }

    public void addScope(String scope) {
        this.scopes.add(scope);
    }
}
