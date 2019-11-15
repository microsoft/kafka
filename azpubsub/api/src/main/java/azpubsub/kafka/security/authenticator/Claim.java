package azpubsub.kafka.security.authenticator;

/**
 * Java Claim class mapped from c# claim class defined in dSTS token
 */
public class Claim {
    private String claimType;
    private String issuer;
    private String originalIssuer;
    private String label;
    private String nameClaimType;
    private String roleClaimType;
    private String value;
    private String valueType;

    public Claim(String ct,
                 String iss,
                 String oriIss,
                 String lbl,
                 String nct,
                 String rct,
                 String v,
                 String vt){
        claimType = ct;
        issuer = iss;
        originalIssuer = oriIss;
        label = lbl;
        nameClaimType = nct;
        roleClaimType = rct;
        value = v;
        valueType = vt;
    }

    public String getClaimType() {
        return claimType;
    }

    public void setClaimType(String type) {
        claimType = type;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String isr) {
        issuer = isr;
    }

    public String getOriginalIssuer() {
        return originalIssuer;
    }

    public void setOriginalIssuer(String isr) {
        originalIssuer = isr;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String lbl) {
        label = lbl;
    }

    public String getNameClaimType() {
        return nameClaimType;
    }

    public void setNameClaimType(String type) {
        nameClaimType = type;
    }

    public String getRoleClaimType () {
        return roleClaimType;
    }

    public void setRoleClaimType (String type) {
        roleClaimType = type;
    }

    public String getValue () {
        return value;
    }

    public void setValue(String val) {
        value = val;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueTye (String type) {
        valueType = type;
    }
}
