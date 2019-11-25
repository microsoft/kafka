package azpubsub.contextvalidator.kafka.security.auth;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;
import java.util.Map;

public class Utils {
    static final String AZPUBSUB_PROPERTIES_PROP = "azpubsub.properties";

    public static Map<String, ?> getAzPubSubConfig(Map<String, ?> configs){

        String azpubsubPropertiesFilePath = System.getProperty(AZPUBSUB_PROPERTIES_PROP);
        try (
            InputStream inputStream = new FileInputStream(new File(azpubsubPropertiesFilePath))) {
            Properties properties = new Properties();

            properties.load(inputStream);
            for (Map.Entry<String, ?> e : configs.entrySet()
            ) {
                if (null != e && null != e.getKey() && null != e.getValue()) {
                    properties.putIfAbsent(e.getKey(), e.getValue());
                }
            }

            @SuppressWarnings({ "unchecked", "rawtypes" })
            Map<String, ?> map = new HashMap(properties);
            return map;
        }
        catch (
        IOException ex) {
            throw new IllegalArgumentException("Failed to read azpubusb properties or merge it with Kafka global configs", ex.getCause());
        }
    }
}
