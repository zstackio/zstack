package org.zstack.sdk;



public class InfoSecSecretResourcePoolInventory extends org.zstack.sdk.SecretResourcePoolInventory {

    public long connectTimeOut;
    public void setConnectTimeOut(long connectTimeOut) {
        this.connectTimeOut = connectTimeOut;
    }
    public long getConnectTimeOut() {
        return this.connectTimeOut;
    }

    public boolean autoCheck;
    public void setAutoCheck(boolean autoCheck) {
        this.autoCheck = autoCheck;
    }
    public boolean getAutoCheck() {
        return this.autoCheck;
    }

    public long checkInterval;
    public void setCheckInterval(long checkInterval) {
        this.checkInterval = checkInterval;
    }
    public long getCheckInterval() {
        return this.checkInterval;
    }

    public java.lang.Integer connectionMode;
    public void setConnectionMode(java.lang.Integer connectionMode) {
        this.connectionMode = connectionMode;
    }
    public java.lang.Integer getConnectionMode() {
        return this.connectionMode;
    }

    public java.lang.String activatedToken;
    public void setActivatedToken(java.lang.String activatedToken) {
        this.activatedToken = activatedToken;
    }
    public java.lang.String getActivatedToken() {
        return this.activatedToken;
    }

    public java.lang.String protectToken;
    public void setProtectToken(java.lang.String protectToken) {
        this.protectToken = protectToken;
    }
    public java.lang.String getProtectToken() {
        return this.protectToken;
    }

    public java.lang.String hmacToken;
    public void setHmacToken(java.lang.String hmacToken) {
        this.hmacToken = hmacToken;
    }
    public java.lang.String getHmacToken() {
        return this.hmacToken;
    }

}
