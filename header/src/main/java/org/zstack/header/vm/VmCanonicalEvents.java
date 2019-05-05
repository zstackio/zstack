package org.zstack.header.vm;

import org.zstack.header.message.NeedJsonSchema;

import java.util.Date;

/**
 * Created by frank on 3/4/2016.
 */
public class VmCanonicalEvents {
    public static final String VM_FULL_STATE_CHANGED_PATH = "/vm/state/change";
    public static final String VM_INSTANCE_OFFERING_CHANGED_PATH = "/vm/instanceoffering/change";
    public static final String VM_CONFIG_CHANGED_PATH = "/vm/config/change";

    @NeedJsonSchema
    public static class VmConfigChangedData {
        private String vmUuid;
        private VmInstanceInventory inv;
        private Date date = new Date();
        private String accountUuid;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public VmInstanceInventory getInv() {
            return inv;
        }

        public void setInv(VmInstanceInventory inv) {
            this.inv = inv;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public String getAccountUuid() {
            return accountUuid;
        }

        public void setAccoundUuid(String accountUuid) {
            this.accountUuid = accountUuid;
        }
    }

    @NeedJsonSchema
    public static class InstanceOfferingChangedData {
        private String vmUuid;
        private String oldInstanceOfferingUuid;
        private String oldInstanceOfferingInventory;
        private String newInstanceOfferingUuid;
        private String newInstanceOfferingInventory;
        private Date date = new Date();

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public String getOldInstanceOfferingUuid() {
            return oldInstanceOfferingUuid;
        }

        public void setOldInstanceOfferingUuid(String oldInstanceOfferingUuid) {
            this.oldInstanceOfferingUuid = oldInstanceOfferingUuid;
        }

        public String getOldInstanceOfferingInventory() {
            return oldInstanceOfferingInventory;
        }

        public void setOldInstanceOfferingInventory(String oldInstanceOfferingInventory) {
            this.oldInstanceOfferingInventory = oldInstanceOfferingInventory;
        }

        public String getNewInstanceOfferingUuid() {
            return newInstanceOfferingUuid;
        }

        public void setNewInstanceOfferingUuid(String newInstanceOfferingUuid) {
            this.newInstanceOfferingUuid = newInstanceOfferingUuid;
        }

        public String getNewInstanceOfferingInventory() {
            return newInstanceOfferingInventory;
        }

        public void setNewInstanceOfferingInventory(String newInstanceOfferingInventory) {
            this.newInstanceOfferingInventory = newInstanceOfferingInventory;
        }
    }

    @NeedJsonSchema
    public static class VmStateChangedData {
        private String vmUuid;
        private String oldState;
        private String newState;
        private VmInstanceInventory inventory;
        private Date date = new Date();

        public VmInstanceInventory getInventory() {
            return inventory;
        }

        public void setInventory(VmInstanceInventory inventory) {
            this.inventory = inventory;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public String getOldState() {
            return oldState;
        }

        public void setOldState(String oldState) {
            this.oldState = oldState;
        }

        public String getNewState() {
            return newState;
        }

        public void setNewState(String newState) {
            this.newState = newState;
        }
    }
}
