package com.microsoft.autopilot.azpubsub;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.autopilot.ApPki;

/**
 * An X509TrustManager backed by Azure AP PKI infrastructure.
 */
public class APPKITrustManager implements X509TrustManager  {
    private static final Logger LOG
            = LoggerFactory.getLogger(APPKITrustManager.class);

    private static final int defaultAuthExpireMinutes = 15;
    private static final int defaultNoAuthExpireMinutes = 1;

    private String serverAcl = "APMF\\AzPubSub.Autopilot.*";

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
        if (chain == null || chain.length == 0) {
            throw new IllegalArgumentException(
                    "Null or empty certificate chain is invalid");
        }

        // For performance reasons we do not verify AP PKI certificate until a request is issued

        return;
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
        if (chain == null || chain.length == 0) {
            throw new IllegalArgumentException(
                    "Null or empty certificate chain is invalid");
        }

        if (!cachedApVerifyCert(chain[0], serverAcl)) {
            throw new CertificateException(
                    "Server certificate verification failed");
        }

        LOG.debug("Server certificate verification succeeded");
        return;
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        LOG.debug("Getting empty accepted issuers");
        return new X509Certificate[0];
    }

    /**
     * Verify an AP PKI X509Certificate against an AP ACL.
     * Caches the result in memory, invalidated according to System property
     * @param cert Unauthenticated X509Certificate provided by the remote host.
     * @param apAcl ACL to authorize remote host against.
     * @return True if certificate identity is authentic and authorized
     * according to AP PKI.
     */
    public static boolean cachedApVerifyCert(X509Certificate cert,
                                             String apAcl) {

    	//return true;
        return ApPki.cachedApVerifyCert(cert, apAcl, defaultAuthExpireMinutes, defaultNoAuthExpireMinutes);
    }
}