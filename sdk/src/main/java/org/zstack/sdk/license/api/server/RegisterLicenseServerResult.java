package org.zstack.sdk.license.api.server;

import org.zstack.sdk.license.header.server.LicenseAuthorizedNodeInventory;
import org.zstack.sdk.license.header.server.LicenseAuthorizedNodeInventory;

public class RegisterLicenseServerResult {
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
