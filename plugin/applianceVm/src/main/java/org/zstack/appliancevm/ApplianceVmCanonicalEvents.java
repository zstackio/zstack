package org.zstack.appliancevm;

import org.zstack.header.errorcode.ErrorCode;

public class ApplianceVmCanonicalEvents {
    public static final String DISCONNECTED_PATH = "/appliance-vm/disconnected";

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
