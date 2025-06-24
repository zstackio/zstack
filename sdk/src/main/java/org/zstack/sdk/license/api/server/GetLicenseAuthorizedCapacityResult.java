package org.zstack.sdk.license.api.server;

import org.zstack.sdk.license.header.server.TotalLicenseAuthorizedCapacityView;
import org.zstack.sdk.license.header.server.LicenseAuthorizedCapacityServerUsageView;

public class GetLicenseAuthorizedCapacityResult {
    public TotalLicenseAuthorizedCapacityView total;
    public void setTotal(TotalLicenseAuthorizedCapacityView total) {
        this.total = total;
    }
    public TotalLicenseAuthorizedCapacityView getTotal() {
        return this.total;
    }

    public java.util.List clients;
    public void setClients(java.util.List clients) {
        this.clients = clients;
    }
    public java.util.List getClients() {
        return this.clients;
    }

    public LicenseAuthorizedCapacityServerUsageView server;
    public void setServer(LicenseAuthorizedCapacityServerUsageView server) {
        this.server = server;
    }
    public LicenseAuthorizedCapacityServerUsageView getServer() {
        return this.server;
    }

}
