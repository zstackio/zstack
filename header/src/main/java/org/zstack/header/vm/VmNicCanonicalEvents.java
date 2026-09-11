package org.zstack.header.vm;

import org.zstack.header.message.NeedJsonSchema;

import java.util.Date;

/**
 * Created by lining on 2018/11/27.
 */
public class VmNicCanonicalEvents {
    public static final String VM_NIC_CREATED_PATH = "/vmNic/vmNicCreated";
    public static final String VM_NIC_DELETED_PATH = "/vmNic/vmNicDeleted";
    public static final String VM_NIC_INFO_CHANGED_PATH = "/vmNic/vmNicInfoChanged";

    public enum VmNicInfoChangeType {
        MAC,
        IP
    }

    @NeedJsonSchema
    public static class VmNicInfoChangedData {
        private String vmInstanceUuid;
        private String vmNicUuid;
        private VmNicInfoChangeType changeType;
        private Date date = new Date();

        public String getVmInstanceUuid() {
            return vmInstanceUuid;
        }

        public void setVmInstanceUuid(String vmInstanceUuid) {
            this.vmInstanceUuid = vmInstanceUuid;
        }

        public String getVmNicUuid() {
            return vmNicUuid;
        }

        public void setVmNicUuid(String vmNicUuid) {
            this.vmNicUuid = vmNicUuid;
        }

        public VmNicInfoChangeType getChangeType() {
            return changeType;
        }

        public void setChangeType(VmNicInfoChangeType changeType) {
            this.changeType = changeType;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }
    }

    @NeedJsonSchema
    public static class VmNicEventData {
        private VmNicInventory inventory;
        private Date date = new Date();
        private String currentStatus;
        private String accountUuid;

        public VmNicInventory getInventory() {
            return inventory;
        }

        public void setInventory(VmNicInventory inventory) {
            this.inventory = inventory;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public String getCurrentStatus() {
            return currentStatus;
        }

        public void setCurrentStatus(String currentStatus) {
            this.currentStatus = currentStatus;
        }

        public String getAccountUuid() {
            return accountUuid;
        }

        public void setAccountUuid(String accountUuid) {
            this.accountUuid = accountUuid;
        }
    }
}
