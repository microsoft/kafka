package com.microsoft.kafka.security.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Java Claim class mapped from c# claim class defined in dSTS token
 */
public class Claim implements Comparable<Claim> {
    private String claimType;
    private String issuer;
    private String originalIssuer;
    private String label;
    private String nameClaimType;
    private String roleClaimType;
    private String value;
    private String valueType;

    @JsonCreator
    public Claim(@JsonProperty("claimType") String ct,
                 @JsonProperty("issuer") String iss,
                 @JsonProperty("originalIssuer") String oriIss,
                 @JsonProperty("label") String lbl,
                 @JsonProperty("nameClaimType") String nct,
                 @JsonProperty("roleClaimType") String rct,
                 @JsonProperty("value") String v,
                 @JsonProperty("valueType") String vt){
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

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + computeHash(value);
        hash = 31 * hash + computeHash(valueType);
        hash = 31 * hash + computeHash(claimType);
        hash = 31 * hash + computeHash(roleClaimType);
        hash = 31 * hash + computeHash(nameClaimType);
        hash = 31 * hash + computeHash(issuer);
        hash = 31 * hash + computeHash(originalIssuer);
        hash = 31 * hash + computeHash(label);

        return hash;
    }

    public int compareTo(Claim another) {
        int ret = this.hashCode() - another.hashCode();
        if(0 == ret) {
            ret = compare(value, another.value);
            if(ret != 0) return ret;

            ret = compare(valueType, another.valueType);
            if(ret != 0) return ret;

            ret = compare(claimType, another.claimType);
            if(ret != 0) return ret;

            ret = compare(roleClaimType, another.roleClaimType);
            if(ret != 0) return ret;

            ret = compare(nameClaimType, another.nameClaimType);
            if(ret != 0) return ret;

            ret = compare(issuer, another.issuer);
            if(ret != 0) return ret;

            ret = compare(originalIssuer, another.originalIssuer);
            if(ret != 0) return ret;

            ret = compare(label, another.label);
            if(ret != 0) return ret;
        }
        return ret;
    }

    private int computeHash(String s) {
        return null == s ? 0 : s.hashCode();
    }

    private int compare(String str1, String str2) {
        if(str1 == null && str2 == null) return 0;
        if(str1 == null) return -compare(str2, null);

        return str2 == null ? 1 : str1.compareTo(str2);
    }
}
