package org.zstack.header.host;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.NeedJsonSchema;

/**
 * Created by xing5 on 2016/3/22.
 */
public class HostCanonicalEvents {
    public static final String HOST_STATUS_CHANGED_PATH = "/host/status/change";
    public static final String HOST_DELETED_PATH = "/host/delete";
    public static final String HOST_DISCONNECTED_PATH = "/host/disconnected";
    public static final String HOST_CHECK_MOUNT_FAULT = "/host/mount/path/fault";
    public static final String HOST_CHECK_INITIALIZED_FAILED = "/host/check/initialized/falied";
    public static final String HOST_USB_STORAGE_DEVICE_DETECTED = "/host/usb_storage/detected";

    @NeedJsonSchema
    public static class HostUsbStorageDeviceData {
        public String hostUuid;
        public String manufacturer;
        public String product;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getManufacturer() {
            return manufacturer;
        }

        public void setManufacturer(String manufacturer) {
            this.manufacturer = manufacturer;
        }

        public String getProduct() {
            return product;
        }

        public void setProduct(String product) {
            this.product = product;
        }
    }

    public static class HostDisconnectedData {
        public String hostUuid;
        public ErrorCode reason;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public ErrorCode getReason() {
            return reason;
        }

        public void setReason(ErrorCode reason) {
            this.reason = reason;
        }
    }

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

    @NeedJsonSchema
    public static class HostMountData {
        public String hostUuid;
        public String psUuid;
        public String details;
        public Long eventTime = System.currentTimeMillis();
    }
}
