package com.microsoft.azpubsub.security.authenticator;

import java.util.Set;

import javax.net.ssl.SSLSession;

import com.microsoft.azpubsub.security.auth.AzPubSubConfig;

/*
 * Interface retrieve the certificate identity
 */
public interface CertificateIdentity {
    public void configure(AzPubSubConfig config);

    public Set<String> getIdentities(SSLSession sslSession);
}
