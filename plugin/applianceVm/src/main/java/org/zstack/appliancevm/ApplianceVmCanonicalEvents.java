package org.zstack.appliancevm;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.NeedJsonSchema;

public class ApplianceVmCanonicalEvents {
    public static final String DISCONNECTED_PATH = "/appliance-vm/disconnected";
    public static final String APPLIANCEVM_STATE_CHANGED_PATH = "/appliance-vm/state/change";
    public static final String APPLIANCEVM_STATUS_CHANGED_PATH = "/appliance-vm/status/change";
    public static final String SERVICE_UNHEALTHY_PATH = "/appliance-vm/sevice/unhealthy";
    public static final String SERVICE_HEALTHY_PATH = "/appliance-vm/sevice/healthy";
    public static final String APPLIANCEVM_HASTATUS_CHANGED_PATH = "/appliance-vm/hastatus/changed";
    public static final String APPLIANCEVM_ABNORMAL_FILE_REPORT_PATH = "/appliance-vm/abnormalfiles/report";

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

    public static class ApplianceVmStateChangeData {
        private String applianceVmUuid;
        private String oldState;
        private String newState;
        private ApplianceVmInventory inv;

        public String getApplianceVmUuid() {
            return applianceVmUuid;
        }

        public void setApplianceVmUuid(String applianceVmUuid) {
            this.applianceVmUuid = applianceVmUuid;
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

    public static class ServiceHealthData {
        private String applianceVmUuid;
        private String applianceVmType;
        private Boolean healthy;
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

        public Boolean getHealthy() {
            return healthy;
        }

        public void setHealthy(Boolean healthy) {
            this.healthy = healthy;
        }
    }

    public static class ApplianceVmHaStatusChangedData {
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

    public static class ApplianceVmAbnormalFilesDate {
        private String applianceVmUuid;
        private String applianceVmType;
        private String abnormalFiles;
        private String diskTotal;
        private String diskUsed;
        private String diskUsedutilization;

        static class AbnormalFile {
            private String filePath;
            private String fileSize;

            public String getFilePath() {
                return filePath;
            }

            public void setFilePath(String filePath) {
                this.filePath = filePath;
            }

            public String getFileSize() {
                return fileSize;
            }

            public void setFileSize(String fileSize) {
                this.fileSize = fileSize;
            }
        }

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

        public String getAbnormalFiles() {
            return abnormalFiles;
        }

        public void setAbnormalFiles(String abnormalFiles) {
            this.abnormalFiles = abnormalFiles;
        }

        public String getDiskTotal() {
            return diskTotal;
        }

        public void setDiskTotal(String diskTotal) {
            this.diskTotal = diskTotal;
        }

        public String getDiskUsed() {
            return diskUsed;
        }

        public void setDiskUsed(String diskUsed) {
            this.diskUsed = diskUsed;
        }

        public String getDiskUsedutilization() {
            return diskUsedutilization;
        }

        public void setDiskUsedutilization(String diskUsedutilization) {
            this.diskUsedutilization = diskUsedutilization;
        }
    }
}
