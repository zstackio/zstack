package org.zstack.sdk.license.api.server;

import org.zstack.sdk.license.header.server.LicenseAuthorizedNodeInventory;
import org.zstack.sdk.license.header.server.LicenseAuthorizedNodeInventory;

public class VerifyLicenseServerResult {
    public java.lang.String accessKeyId;
    public void setAccessKeyId(java.lang.String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }
    public java.lang.String getAccessKeyId() {
        return this.accessKeyId;
    }

    public java.lang.String accessKeySecret;
    public void setAccessKeySecret(java.lang.String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }
    public java.lang.String getAccessKeySecret() {
        return this.accessKeySecret;
    }

    public LicenseAuthorizedNodeInventory licenseClient;
    public void setLicenseClient(LicenseAuthorizedNodeInventory licenseClient) {
        this.licenseClient = licenseClient;
    }
    public LicenseAuthorizedNodeInventory getLicenseClient() {
        return this.licenseClient;
    }

    public LicenseAuthorizedNodeInventory licenseServer;
    public void setLicenseServer(LicenseAuthorizedNodeInventory licenseServer) {
        this.licenseServer = licenseServer;
    }
    public LicenseAuthorizedNodeInventory getLicenseServer() {
        return this.licenseServer;
    }

}
