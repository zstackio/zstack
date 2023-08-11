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
    public static final String HOST_PHYSICAL_MEMORY_ECC_ERROR_TRIGGERED = "/host/physicalMemory/ecc/error/triggered";
    public static final String HOST_PHYSICAL_CPU_STATUS_ABNORMAL = "/host/physicalCpu/status/abnormal";
    public static final String HOST_PHYSICAL_MEMORY_STATUS_ABNORMAL = "/host/physicalMemory/status/abnormal";
    public static final String HOST_PHYSICAL_FAN_STATUS_ABNORMAL = "/host/physicalFan/status/abnormal";
    public static final String HOST_PHYSICAL_DISK_STATUS_ABNORMAL = "/host/physicalDisk/status/abnormal";
    public static final String HOST_PHYSICAL_DISK_INSERT_TRIGGERED = "/host/physicalDisk/insert/triggered";
    public static final String HOST_PHYSICAL_DISK_REMOVE_TRIGGERED = "/host/physicalDisk/remove/triggered";
    public static final String HOST_PHYSICAL_HBA_STATE_ABNORMAL = "/host/physicalHBA/state/abnormal";

    @NeedJsonSchema
    public static class HostPhysicalCpuStatusAbnormalData {
        private String hostUuid;
        private String cpuName;
        private String status;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getCpuName() {
            return cpuName;
        }

        public void setCpuName(String cpuName) {
            this.cpuName = cpuName;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    @NeedJsonSchema
    public static class HostPhysicalMemoryStatusAbnormalData {
        private String hostUuid;
        private String locator;
        private String status;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getLocator() {
            return locator;
        }

        public void setLocator(String locator) {
            this.locator = locator;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    @NeedJsonSchema
    public static class HostPhysicalFanStatusAbnormalData {
        private String hostUuid;
        private String fanName;
        private String status;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getFanName() {
            return fanName;
        }

        public void setFanName(String fanName) {
            this.fanName = fanName;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    @NeedJsonSchema
    public static class HostPhysicalDiskStatusAbnormalData {
        private String hostUuid;
        private String serialNumber;
        private String status;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getSerialNumber() {
            return serialNumber;
        }

        public void setSerialNumber(String serialNumber) {
            this.serialNumber = serialNumber;
        }


        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    @NeedJsonSchema
    public static class HostPhysicalDiskData {
        private String hostUuid;

        private String serialNumber;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getSerialNumber() {
            return serialNumber;
        }

        public void setSerialNumber(String serialNumber) {
            this.serialNumber = serialNumber;
        }
    }

    @NeedJsonSchema
    public static class HostPhysicalMemoryEccErrorData {
        private String hostUuid;
        private ErrorCode detail;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public ErrorCode getDetail() {
            return detail;
        }

        public void setDetail(ErrorCode detail) {
            this.detail = detail;
        }
    }

    @NeedJsonSchema
    public static class HostPhysicalHBAPortStateAbnormalData {
        private String hostUuid;

        private String status;

        private String portName;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getPortName() {
            return portName;
        }

        public void setPortName(String portName) {
            this.portName = portName;
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
