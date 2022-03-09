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
    public static final String HOST_CHECK_INITIALIZED_FAILED = "/host/check/initialized/failed";
    public static final String HOST_PHYSICAL_NIC_STATUS_UP = "/host/physicalNic/status/up";
    public static final String HOST_PHYSICAL_NIC_STATUS_DOWN = "/host/physicalNic/status/down";

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

    @NeedJsonSchema
    public static class HostPhysicalNicStatusData {
        public String hostUuid;
        public String hostAddr;
        public String fromBond;
        public String interfaceName;
        public String ipAddress;
        public String interfaceStatus;
        public Long eventTime = System.currentTimeMillis();


        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getHostAddr() {
            return hostAddr;
        }

        public void setHostAddr(String hostAddr) {
            this.hostAddr = hostAddr;
        }

        public String getFromBond() {
            return fromBond;
        }

        public void setFromBond(String fromBond) {
            this.fromBond = fromBond;
        }

        public String getInterfaceName() {
            return interfaceName;
        }

        public void setInterfaceName(String interfaceName) {
            this.interfaceName = interfaceName;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public void setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
        }

        public String getInterfaceStatus() {
            return interfaceStatus;
        }

        public void setInterfaceStatus(String interfaceStatus) {
            this.interfaceStatus = interfaceStatus;
        }
    }
}
