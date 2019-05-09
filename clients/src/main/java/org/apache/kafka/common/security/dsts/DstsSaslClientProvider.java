package org.apache.kafka.common.security.dsts;

import java.security.Provider;
import java.security.Security;

import org.apache.kafka.common.security.dsts.DstsSaslClient.DstsSaslClientFactory;

public class DstsSaslClientProvider extends Provider {
    private static final long DserialVersionUID = 1L;

    @SuppressWarnings("deprecation")
    protected DstsSaslClientProvider() {
        super("Simple SASL/DSTS Client Provider", 1.0, "Simple SASL/DSTS Client Provider for Kafka");
        put("SaslClientFactory.DSTS", DstsSaslClientFactory.class.getName());
    }

    public static void initialize() {
        Security.addProvider(new DstsSaslClientProvider());
    }
}
