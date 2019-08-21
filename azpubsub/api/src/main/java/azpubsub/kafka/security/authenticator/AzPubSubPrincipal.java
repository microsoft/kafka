package azpubsub.kafka.security.authenticator;

/**
 * AzPubSubPrincipal class.
 */
public class AzPubSubPrincipal {
    private String principalType;  //USER or TOKEN
    private String principalName;  //"ANONYMOUS" for USER Type or Token object (in compute-move: /src/Services/Kafka/DstsAuthenticationAuthorization/clients/src/main/java/com/microsoft/kafka/common/security/saml/Token.java) JSON string for TOKEN Type.

    public AzPubSubPrincipal(String type, String name) {
        principalName = name;
        principalType = type;
    }

    public String getPrincipalType() {
        return principalType;
    }

    public String getPrincipalName() {
        return principalName;
    }
}
