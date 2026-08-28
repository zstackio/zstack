package org.zstack.sdk.network.zns;

import org.zstack.sdk.L2NetworkInventory;
import org.zstack.sdk.L3NetworkInventory;
import org.zstack.sdk.network.zns.ZnsSegmentSyncOperationInventory;

public class ZnsSegmentCloudProjectionInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String sdnControllerUuid;
    public void setSdnControllerUuid(java.lang.String sdnControllerUuid) {
        this.sdnControllerUuid = sdnControllerUuid;
    }
    public java.lang.String getSdnControllerUuid() {
        return this.sdnControllerUuid;
    }

    public java.lang.String znsSegmentUuid;
    public void setZnsSegmentUuid(java.lang.String znsSegmentUuid) {
        this.znsSegmentUuid = znsSegmentUuid;
    }
    public java.lang.String getZnsSegmentUuid() {
        return this.znsSegmentUuid;
    }

    public java.lang.String zoneUuid;
    public void setZoneUuid(java.lang.String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }
    public java.lang.String getZoneUuid() {
        return this.zoneUuid;
    }

    public java.lang.String l2NetworkUuid;
    public void setL2NetworkUuid(java.lang.String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }
    public java.lang.String getL2NetworkUuid() {
        return this.l2NetworkUuid;
    }

    public java.lang.String l3NetworkUuid;
    public void setL3NetworkUuid(java.lang.String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }
    public java.lang.String getL3NetworkUuid() {
        return this.l3NetworkUuid;
    }

    public java.lang.String state;
    public void setState(java.lang.String state) {
        this.state = state;
    }
    public java.lang.String getState() {
        return this.state;
    }

    public java.lang.Long currentConfigVersion;
    public void setCurrentConfigVersion(java.lang.Long currentConfigVersion) {
        this.currentConfigVersion = currentConfigVersion;
    }
    public java.lang.Long getCurrentConfigVersion() {
        return this.currentConfigVersion;
    }

    public java.lang.Long appliedConfigVersion;
    public void setAppliedConfigVersion(java.lang.Long appliedConfigVersion) {
        this.appliedConfigVersion = appliedConfigVersion;
    }
    public java.lang.Long getAppliedConfigVersion() {
        return this.appliedConfigVersion;
    }

    public java.lang.String operationUuid;
    public void setOperationUuid(java.lang.String operationUuid) {
        this.operationUuid = operationUuid;
    }
    public java.lang.String getOperationUuid() {
        return this.operationUuid;
    }

    public java.lang.String operationStep;
    public void setOperationStep(java.lang.String operationStep) {
        this.operationStep = operationStep;
    }
    public java.lang.String getOperationStep() {
        return this.operationStep;
    }

    public java.lang.String lastErrorCode;
    public void setLastErrorCode(java.lang.String lastErrorCode) {
        this.lastErrorCode = lastErrorCode;
    }
    public java.lang.String getLastErrorCode() {
        return this.lastErrorCode;
    }

    public java.lang.String lastErrorDetails;
    public void setLastErrorDetails(java.lang.String lastErrorDetails) {
        this.lastErrorDetails = lastErrorDetails;
    }
    public java.lang.String getLastErrorDetails() {
        return this.lastErrorDetails;
    }

    public java.sql.Timestamp createDate;
    public void setCreateDate(java.sql.Timestamp createDate) {
        this.createDate = createDate;
    }
    public java.sql.Timestamp getCreateDate() {
        return this.createDate;
    }

    public java.sql.Timestamp lastOpDate;
    public void setLastOpDate(java.sql.Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
    public java.sql.Timestamp getLastOpDate() {
        return this.lastOpDate;
    }

    public L2NetworkInventory l2Network;
    public void setL2Network(L2NetworkInventory l2Network) {
        this.l2Network = l2Network;
    }
    public L2NetworkInventory getL2Network() {
        return this.l2Network;
    }

    public L3NetworkInventory l3Network;
    public void setL3Network(L3NetworkInventory l3Network) {
        this.l3Network = l3Network;
    }
    public L3NetworkInventory getL3Network() {
        return this.l3Network;
    }

    public ZnsSegmentSyncOperationInventory operation;
    public void setOperation(ZnsSegmentSyncOperationInventory operation) {
        this.operation = operation;
    }
    public ZnsSegmentSyncOperationInventory getOperation() {
        return this.operation;
    }

}
