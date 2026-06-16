package org.zstack.sdk;

import org.zstack.sdk.LicenseInventory;

public class LicenseAuthorizationInfo  {

    public java.lang.String authType;
    public void setAuthType(java.lang.String authType) {
        this.authType = authType;
    }
    public java.lang.String getAuthType() {
        return this.authType;
    }

    public boolean connected;
    public void setConnected(boolean connected) {
        this.connected = connected;
    }
    public boolean getConnected() {
        return this.connected;
    }

    public java.lang.String serverUrl;
    public void setServerUrl(java.lang.String serverUrl) {
        this.serverUrl = serverUrl;
    }
    public java.lang.String getServerUrl() {
        return this.serverUrl;
    }

    public java.lang.String serverVersion;
    public void setServerVersion(java.lang.String serverVersion) {
        this.serverVersion = serverVersion;
    }
    public java.lang.String getServerVersion() {
        return this.serverVersion;
    }

    public java.lang.String localVersion;
    public void setLocalVersion(java.lang.String localVersion) {
        this.localVersion = localVersion;
    }
    public java.lang.String getLocalVersion() {
        return this.localVersion;
    }

    public java.lang.String siteUuid;
    public void setSiteUuid(java.lang.String siteUuid) {
        this.siteUuid = siteUuid;
    }
    public java.lang.String getSiteUuid() {
        return this.siteUuid;
    }

    public java.lang.String siteName;
    public void setSiteName(java.lang.String siteName) {
        this.siteName = siteName;
    }
    public java.lang.String getSiteName() {
        return this.siteName;
    }

    public java.lang.String productLine;
    public void setProductLine(java.lang.String productLine) {
        this.productLine = productLine;
    }
    public java.lang.String getProductLine() {
        return this.productLine;
    }

    public java.lang.String snapshotState;
    public void setSnapshotState(java.lang.String snapshotState) {
        this.snapshotState = snapshotState;
    }
    public java.lang.String getSnapshotState() {
        return this.snapshotState;
    }

    public java.lang.String source;
    public void setSource(java.lang.String source) {
        this.source = source;
    }
    public java.lang.String getSource() {
        return this.source;
    }

    public java.lang.String lastSyncDate;
    public void setLastSyncDate(java.lang.String lastSyncDate) {
        this.lastSyncDate = lastSyncDate;
    }
    public java.lang.String getLastSyncDate() {
        return this.lastSyncDate;
    }

    public LicenseInventory platformLicense;
    public void setPlatformLicense(LicenseInventory platformLicense) {
        this.platformLicense = platformLicense;
    }
    public LicenseInventory getPlatformLicense() {
        return this.platformLicense;
    }

    public java.util.List addOns;
    public void setAddOns(java.util.List addOns) {
        this.addOns = addOns;
    }
    public java.util.List getAddOns() {
        return this.addOns;
    }

    public java.util.List quotas;
    public void setQuotas(java.util.List quotas) {
        this.quotas = quotas;
    }
    public java.util.List getQuotas() {
        return this.quotas;
    }

}
