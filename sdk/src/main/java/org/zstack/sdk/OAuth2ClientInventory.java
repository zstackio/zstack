package org.zstack.sdk;



public class OAuth2ClientInventory extends org.zstack.sdk.SSOClientInventory {

    public java.lang.String clientId;
    public void setClientId(java.lang.String clientId) {
        this.clientId = clientId;
    }
    public java.lang.String getClientId() {
        return this.clientId;
    }

    public java.lang.String clientSecret;
    public void setClientSecret(java.lang.String clientSecret) {
        this.clientSecret = clientSecret;
    }
    public java.lang.String getClientSecret() {
        return this.clientSecret;
    }

    public java.lang.String authorizationUrl;
    public void setAuthorizationUrl(java.lang.String authorizationUrl) {
        this.authorizationUrl = authorizationUrl;
    }
    public java.lang.String getAuthorizationUrl() {
        return this.authorizationUrl;
    }

    public java.lang.String tokenUrl;
    public void setTokenUrl(java.lang.String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }
    public java.lang.String getTokenUrl() {
        return this.tokenUrl;
    }

    public java.lang.String grantType;
    public void setGrantType(java.lang.String grantType) {
        this.grantType = grantType;
    }
    public java.lang.String getGrantType() {
        return this.grantType;
    }

}
