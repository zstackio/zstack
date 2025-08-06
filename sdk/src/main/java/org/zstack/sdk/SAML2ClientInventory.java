package org.zstack.sdk;

import org.zstack.sdk.SAML2State;

public class SAML2ClientInventory extends org.zstack.sdk.SSOClientInventory {

    public java.lang.String idpMetadataBase64;
    public void setIdpMetadataBase64(java.lang.String idpMetadataBase64) {
        this.idpMetadataBase64 = idpMetadataBase64;
    }
    public java.lang.String getIdpMetadataBase64() {
        return this.idpMetadataBase64;
    }

    public java.lang.String spX509Certificate;
    public void setSpX509Certificate(java.lang.String spX509Certificate) {
        this.spX509Certificate = spX509Certificate;
    }
    public java.lang.String getSpX509Certificate() {
        return this.spX509Certificate;
    }

    public java.lang.String spMetadataUrl;
    public void setSpMetadataUrl(java.lang.String spMetadataUrl) {
        this.spMetadataUrl = spMetadataUrl;
    }
    public java.lang.String getSpMetadataUrl() {
        return this.spMetadataUrl;
    }

    public SAML2State state;
    public void setState(SAML2State state) {
        this.state = state;
    }
    public SAML2State getState() {
        return this.state;
    }

}
