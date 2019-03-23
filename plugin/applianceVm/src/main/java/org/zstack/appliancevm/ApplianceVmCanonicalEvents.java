package org.zstack.appliancevm;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.NeedJsonSchema;

public class ApplianceVmCanonicalEvents {
    public static final String DISCONNECTED_PATH = "/appliance-vm/disconnected";
    public static final String APPLIANCEVM_STATUS_CHANGED_PATH = "/appliance-vm/status/change";

    @NeedJsonSchema
    public static class ApplianceVmStatusChangedData {
        private String applianceVmUuid;
        private String oldStatus;
        private String newStatus;
        private ApplianceVmInventory inv;

        public String getApplianceVmUuid() {
            return applianceVmUuid;
        }

        public void setApplianceVmUuid(String applianceVmUuid) {
            this.applianceVmUuid = applianceVmUuid;
        }

        public String getOldStatus() {
            return oldStatus;
        }

        public void setOldStatus(String oldStatus) {
            this.oldStatus = oldStatus;
        }

        public String getNewStatus() {
            return newStatus;
        }

        public void setNewStatus(String newStatus) {
            this.newStatus = newStatus;
        }

        public ApplianceVmInventory getInv() {
            return inv;
        }

        public void setInv(ApplianceVmInventory inv) {
            this.inv = inv;
        }
    }

    public static class DisconnectedData {
        private String applianceVmUuid;
        private String applianceVmType;
        private ErrorCode reason;

        public String getApplianceVmUuid() {
            return applianceVmUuid;
        }

        public void setApplianceVmUuid(String applianceVmUuid) {
            this.applianceVmUuid = applianceVmUuid;
        }

        public String getApplianceVmType() {
            return applianceVmType;
        }

        public void setApplianceVmType(String applianceVmType) {
            this.applianceVmType = applianceVmType;
        }

        public ErrorCode getReason() {
            return reason;
        }

        public void setReason(ErrorCode reason) {
            this.reason = reason;
        }
    }
}
