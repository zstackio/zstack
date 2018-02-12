package org.zstack.header.host;

import org.zstack.header.message.NeedJsonSchema;

/**
 * Created by xing5 on 2016/3/22.
 */
public class HostCanonicalEvents {
    public static final String HOST_STATUS_CHANGED_PATH = "/host/status/change";
    public static final String HOST_DELETED_PATH = "/host/delete";

    @NeedJsonSchema
    public static class HostStatusChangedData {
        private String hostUuid;
        private String oldStatus;
        private String newStatus;
        private HostInventory inventory;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
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

        public HostInventory getInventory() {
            return inventory;
        }

        public void setInventory(HostInventory inventory) {
            this.inventory = inventory;
        }
    }

    @NeedJsonSchema
    public static class HostDeletedData {
        private String hostUuid;
        private HostInventory inventory;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public HostInventory getInventory() {
            return inventory;
        }

        public void setInventory(HostInventory inventory) {
            this.inventory = inventory;
        }
    }
}
