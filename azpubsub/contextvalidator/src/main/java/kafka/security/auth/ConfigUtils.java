package azpubsub.contextvalidator.kafka.security.auth;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;
import java.util.Map;

public class ConfigUtils {
    public static final String AZPUBSUB_PROPERTIES_PROP = "azpubsub.properties";
    public static final String AzpubsubTokenValidatorClassPathKey = "azpubsub.token.validator.class";

    public static final String AzpubsubValidateTokenInMinutesProp = "azpubsub.validate.token.in.minutes";
    public static int AzpubsubValidateTokenInMinutes = 60;

    public static final String AzpubsubClientCertificateAclProp = "azpubsub.client.ceritificate.acl";
    public static final String AzpubsubSslAuthenticationValidatorClassProp = "azpubsub.ssl.authentication.validator.class";
    public static final String AzPubSubSaslAuthenticationValidatorClassProp = "azpubsub.sasl.authentication.validator.class";
    public static final String AzPubSubPrincipalComparatorClassProp = "azpubsub.principal.comparator.class";
    public static final String AzPubSubTopicWhiteListProp = "azpubsub.topic.whitelist";
    public static final String AzPubSubDstsMetadataConfigIniProp = "azpubsub.dsts.metadata.config.ini";

    public static Map<String, String> loadAzPubsubConfigAndMergeGlobalConfig(Map<String, ?> configs){

        String azpubsubPropertiesFilePath = System.getProperty(AZPUBSUB_PROPERTIES_PROP);
        try (
            InputStream inputStream = new FileInputStream(new File(azpubsubPropertiesFilePath))) {
            Properties properties = new Properties();

            properties.load(inputStream);
            if(null != configs) {
                for (Map.Entry<String, ?> e : configs.entrySet() ) {
                    if (null != e && null != e.getKey() && null != e.getValue()) {
                        properties.putIfAbsent(e.getKey(), e.getValue());
                    }
                }
            }

            @SuppressWarnings({ "unchecked", "rawtypes" })
            Map<String, String> map = new HashMap(properties);
            return map;
        }
        catch (
        IOException ex) {
            throw new IllegalArgumentException("Failed to read azpubusb properties or merge it with Kafka global configs", ex.getCause());
        }
    }
}
