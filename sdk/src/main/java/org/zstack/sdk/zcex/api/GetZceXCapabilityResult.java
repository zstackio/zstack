package org.zstack.sdk.zcex.api;

import org.zstack.sdk.zcex.entity.ZceXLicenseView;
import org.zstack.sdk.zcex.entity.ZceXClusterView;
import org.zstack.sdk.zcex.entity.ZceXSystemView;

public class GetZceXCapabilityResult {
    public ZceXLicenseView licenses;
    public void setLicenses(ZceXLicenseView licenses) {
        this.licenses = licenses;
    }
    public ZceXLicenseView getLicenses() {
        return this.licenses;
    }

    public ZceXClusterView cluster;
    public void setCluster(ZceXClusterView cluster) {
        this.cluster = cluster;
    }
    public ZceXClusterView getCluster() {
        return this.cluster;
    }

    public ZceXSystemView system;
    public void setSystem(ZceXSystemView system) {
        this.system = system;
    }
    public ZceXSystemView getSystem() {
        return this.system;
    }

}
