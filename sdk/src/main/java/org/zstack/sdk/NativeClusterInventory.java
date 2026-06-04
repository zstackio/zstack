package org.zstack.sdk;



public class NativeClusterInventory extends org.zstack.sdk.ClusterInventory {

    public java.lang.String bizUrl;
    public void setBizUrl(java.lang.String bizUrl) {
        this.bizUrl = bizUrl;
    }
    public java.lang.String getBizUrl() {
        return this.bizUrl;
    }

    public java.lang.String masterUrl;
    public void setMasterUrl(java.lang.String masterUrl) {
        this.masterUrl = masterUrl;
    }
    public java.lang.String getMasterUrl() {
        return this.masterUrl;
    }

    public java.lang.String kubeConfig;
    public void setKubeConfig(java.lang.String kubeConfig) {
        this.kubeConfig = kubeConfig;
    }
    public java.lang.String getKubeConfig() {
        return this.kubeConfig;
    }

    public java.lang.String prometheusURL;
    public void setPrometheusURL(java.lang.String prometheusURL) {
        this.prometheusURL = prometheusURL;
    }
    public java.lang.String getPrometheusURL() {
        return this.prometheusURL;
    }

    public java.lang.String version;
    public void setVersion(java.lang.String version) {
        this.version = version;
    }
    public java.lang.String getVersion() {
        return this.version;
    }

    public java.lang.Integer nodeCount;
    public void setNodeCount(java.lang.Integer nodeCount) {
        this.nodeCount = nodeCount;
    }
    public java.lang.Integer getNodeCount() {
        return this.nodeCount;
    }

    public java.lang.String createType;
    public void setCreateType(java.lang.String createType) {
        this.createType = createType;
    }
    public java.lang.String getCreateType() {
        return this.createType;
    }

    public java.lang.String status;
    public void setStatus(java.lang.String status) {
        this.status = status;
    }
    public java.lang.String getStatus() {
        return this.status;
    }

    public java.lang.String zakuHealthStatus;
    public void setZakuHealthStatus(java.lang.String zakuHealthStatus) {
        this.zakuHealthStatus = zakuHealthStatus;
    }
    public java.lang.String getZakuHealthStatus() {
        return this.zakuHealthStatus;
    }

    public long id;
    public void setId(long id) {
        this.id = id;
    }
    public long getId() {
        return this.id;
    }

}
