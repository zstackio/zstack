package org.zstack.sdk.sns.platform.snmp;



public class SNSSnmpPlatformInventory extends org.zstack.sdk.sns.SNSApplicationPlatformInventory {

    public java.lang.String snmpAddress;
    public void setSnmpAddress(java.lang.String snmpAddress) {
        this.snmpAddress = snmpAddress;
    }
    public java.lang.String getSnmpAddress() {
        return this.snmpAddress;
    }

    public int snmpPort;
    public void setSnmpPort(int snmpPort) {
        this.snmpPort = snmpPort;
    }
    public int getSnmpPort() {
        return this.snmpPort;
    }

    public java.lang.String version;
    public void setVersion(java.lang.String version) {
        this.version = version;
    }
    public java.lang.String getVersion() {
        return this.version;
    }

    public boolean communityConfigured;
    public void setCommunityConfigured(boolean communityConfigured) {
        this.communityConfigured = communityConfigured;
    }
    public boolean getCommunityConfigured() {
        return this.communityConfigured;
    }

    public java.lang.String userName;
    public void setUserName(java.lang.String userName) {
        this.userName = userName;
    }
    public java.lang.String getUserName() {
        return this.userName;
    }

    public boolean authEnabled;
    public void setAuthEnabled(boolean authEnabled) {
        this.authEnabled = authEnabled;
    }
    public boolean getAuthEnabled() {
        return this.authEnabled;
    }

    public java.lang.String authAlgorithm;
    public void setAuthAlgorithm(java.lang.String authAlgorithm) {
        this.authAlgorithm = authAlgorithm;
    }
    public java.lang.String getAuthAlgorithm() {
        return this.authAlgorithm;
    }

    public boolean authPasswordConfigured;
    public void setAuthPasswordConfigured(boolean authPasswordConfigured) {
        this.authPasswordConfigured = authPasswordConfigured;
    }
    public boolean getAuthPasswordConfigured() {
        return this.authPasswordConfigured;
    }

    public boolean privacyEnabled;
    public void setPrivacyEnabled(boolean privacyEnabled) {
        this.privacyEnabled = privacyEnabled;
    }
    public boolean getPrivacyEnabled() {
        return this.privacyEnabled;
    }

    public java.lang.String privacyAlgorithm;
    public void setPrivacyAlgorithm(java.lang.String privacyAlgorithm) {
        this.privacyAlgorithm = privacyAlgorithm;
    }
    public java.lang.String getPrivacyAlgorithm() {
        return this.privacyAlgorithm;
    }

    public boolean privacyPasswordConfigured;
    public void setPrivacyPasswordConfigured(boolean privacyPasswordConfigured) {
        this.privacyPasswordConfigured = privacyPasswordConfigured;
    }
    public boolean getPrivacyPasswordConfigured() {
        return this.privacyPasswordConfigured;
    }

    public java.lang.String securityLevel;
    public void setSecurityLevel(java.lang.String securityLevel) {
        this.securityLevel = securityLevel;
    }
    public java.lang.String getSecurityLevel() {
        return this.securityLevel;
    }

}
