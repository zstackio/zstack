package org.zstack.sdk;

import org.zstack.sdk.CasState;

public class CasClientInventory extends org.zstack.sdk.SSOClientInventory {

    public java.lang.String casServerLoginUrl;
    public void setCasServerLoginUrl(java.lang.String casServerLoginUrl) {
        this.casServerLoginUrl = casServerLoginUrl;
    }
    public java.lang.String getCasServerLoginUrl() {
        return this.casServerLoginUrl;
    }

    public java.lang.String casServerUrlPrefix;
    public void setCasServerUrlPrefix(java.lang.String casServerUrlPrefix) {
        this.casServerUrlPrefix = casServerUrlPrefix;
    }
    public java.lang.String getCasServerUrlPrefix() {
        return this.casServerUrlPrefix;
    }

    public java.lang.String serverName;
    public void setServerName(java.lang.String serverName) {
        this.serverName = serverName;
    }
    public java.lang.String getServerName() {
        return this.serverName;
    }

    public CasState state;
    public void setState(CasState state) {
        this.state = state;
    }
    public CasState getState() {
        return this.state;
    }

}
