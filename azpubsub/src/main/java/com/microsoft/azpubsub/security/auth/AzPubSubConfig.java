package com.microsoft.azpubsub.security.auth;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.kafka.common.utils.Utils;

public class AzPubSubConfig extends AbstractConfig {
    private static final ConfigDef CONFIG;
    private static final String AZPUBSUB_PROPERTIES_PROP = "azpubsub.properties";

    public static final String TOKEN_VALIDATOR_CLASS_CONFIG = "azpubsub.oauth.validator.class";
    private static final String TOKEN_VALIDATOR_CLASS_DOC = "";

    static {
        CONFIG = new ConfigDef().define(TOKEN_VALIDATOR_CLASS_CONFIG,
                                        Type.STRING,
                                		"", 
                                        Importance.MEDIUM,
                                        TOKEN_VALIDATOR_CLASS_DOC)
                                ;
    }

    public static AzPubSubConfig fromProps(Map<String, ?> configProviderProps) {
    	try {
    		Properties props = Utils.loadProps(AzPubSubConfig.AZPUBSUB_PROPERTIES_PROP);
        	return new AzPubSubConfig(props, configProviderProps);
    	} catch (IOException ex) {
            throw new IllegalArgumentException("Failed to read azpubusb properties or merge it with Kafka global configs", ex.getCause());
        }
    }

    public AzPubSubConfig(Map<?, ?> originals,  Map<String, ?> configProviderProps) {
        super(CONFIG, originals, configProviderProps, false);
    }
}
