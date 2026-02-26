package org.zstack.sdk;

import org.zstack.sdk.CertificateInfo;
import org.zstack.sdk.KmsIdentityInventory;

public class KmsInventory extends org.zstack.sdk.KeyProviderInventory {

    public java.lang.String endpoint;
    public void setEndpoint(java.lang.String endpoint) {
        this.endpoint = endpoint;
    }
    public java.lang.String getEndpoint() {
        return this.endpoint;
    }

    public java.lang.Integer port;
    public void setPort(java.lang.Integer port) {
        this.port = port;
    }
    public java.lang.Integer getPort() {
        return this.port;
    }

    public java.lang.String kmipVersion;
    public void setKmipVersion(java.lang.String kmipVersion) {
        this.kmipVersion = kmipVersion;
    }
    public java.lang.String getKmipVersion() {
        return this.kmipVersion;
    }

    public java.lang.String username;
    public void setUsername(java.lang.String username) {
        this.username = username;
    }
    public java.lang.String getUsername() {
        return this.username;
    }

    public boolean trusted;
    public void setTrusted(boolean trusted) {
        this.trusted = trusted;
    }
    public boolean getTrusted() {
        return this.trusted;
    }

    public java.lang.String activeIdentityUuid;
    public void setActiveIdentityUuid(java.lang.String activeIdentityUuid) {
        this.activeIdentityUuid = activeIdentityUuid;
    }
    public java.lang.String getActiveIdentityUuid() {
        return this.activeIdentityUuid;
    }

    public java.lang.String serverCertPem;
    public void setServerCertPem(java.lang.String serverCertPem) {
        this.serverCertPem = serverCertPem;
    }
    public java.lang.String getServerCertPem() {
        return this.serverCertPem;
    }

    public CertificateInfo serverCertInfo;
    public void setServerCertInfo(CertificateInfo serverCertInfo) {
        this.serverCertInfo = serverCertInfo;
    }
    public CertificateInfo getServerCertInfo() {
        return this.serverCertInfo;
    }

    public KmsIdentityInventory activeIdentity;
    public void setActiveIdentity(KmsIdentityInventory activeIdentity) {
        this.activeIdentity = activeIdentity;
    }
    public KmsIdentityInventory getActiveIdentity() {
        return this.activeIdentity;
    }

}
