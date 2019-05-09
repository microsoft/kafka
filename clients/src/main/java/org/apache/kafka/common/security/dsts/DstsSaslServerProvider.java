package org.apache.kafka.common.security.dsts;

import java.security.Provider;
import java.security.Security;

import org.apache.kafka.common.security.dsts.DstsSaslServer.DstsSaslServerFactory;

public class DstsSaslServerProvider extends Provider {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("deprecation")
    protected DstsSaslServerProvider() {
        super("SASL/SCRAM Server Provider", 1.0, "SASL/SCRAM Server Provider for Kafka");
        put("SaslServerFactory.DSTS", DstsSaslServerFactory.class.getName());
    }

    public static void initialize() {
        Security.addProvider(new DstsSaslServerProvider());
    }
}
