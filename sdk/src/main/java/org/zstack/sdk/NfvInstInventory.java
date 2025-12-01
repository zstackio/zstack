package org.zstack.sdk;



public class NfvInstInventory extends org.zstack.sdk.ApplianceVmInventory {

    public int configVersion;
    public void setConfigVersion(int configVersion) {
        this.configVersion = configVersion;
    }
    public int getConfigVersion() {
        return this.configVersion;
    }

    public java.lang.String nfvInstGroupUuid;
    public void setNfvInstGroupUuid(java.lang.String nfvInstGroupUuid) {
        this.nfvInstGroupUuid = nfvInstGroupUuid;
    }
    public java.lang.String getNfvInstGroupUuid() {
        return this.nfvInstGroupUuid;
    }

    public java.lang.String netOsDistro;
    public void setNetOsDistro(java.lang.String netOsDistro) {
        this.netOsDistro = netOsDistro;
    }
    public java.lang.String getNetOsDistro() {
        return this.netOsDistro;
    }

    public java.lang.String baseOsDistro;
    public void setBaseOsDistro(java.lang.String baseOsDistro) {
        this.baseOsDistro = baseOsDistro;
    }
    public java.lang.String getBaseOsDistro() {
        return this.baseOsDistro;
    }

    public java.lang.String clusterStatus;
    public void setClusterStatus(java.lang.String clusterStatus) {
        this.clusterStatus = clusterStatus;
    }
    public java.lang.String getClusterStatus() {
        return this.clusterStatus;
    }

    public java.lang.String statusDetail;
    public void setStatusDetail(java.lang.String statusDetail) {
        this.statusDetail = statusDetail;
    }
    public java.lang.String getStatusDetail() {
        return this.statusDetail;
    }

}
