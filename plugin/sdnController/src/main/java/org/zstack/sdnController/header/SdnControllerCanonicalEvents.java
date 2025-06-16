package org.zstack.sdnController.header;

import org.zstack.header.message.NeedJsonSchema;
import org.zstack.header.network.sdncontroller.SdnControllerInventory;

public class SdnControllerCanonicalEvents {
    public static final String SDNCONTROLLER_STATE_CHANGED_PATH = "/sdnController/state/change";
    public static final String SDNCONTROLLER_STATUS_CHANGED_PATH = "/sdnController/status/change";

    @NeedJsonSchema
    public static class SdnControllerStatusChangedData {
        private String sdnControllerUuid;
        private String sdnControllerType;
        private String oldStatus;
        private String newStatus;
        private SdnControllerInventory inv;

        public String getSdnControllerUuid() {
            return sdnControllerUuid;
        }

        public void setSdnControllerUuid(String sdnControllerUuid) {
            this.sdnControllerUuid = sdnControllerUuid;
        }

        public String getSdnControllerType() {
            return sdnControllerType;
        }

        public void setSdnControllerType(String sdnControllerType) {
            this.sdnControllerType = sdnControllerType;
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

        public SdnControllerInventory getInv() {
            return inv;
        }

        public void setInv(SdnControllerInventory inv) {
            this.inv = inv;
        }
    }

}
