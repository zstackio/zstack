package org.zstack.xinfini.sdk.node;

import org.zstack.xinfini.sdk.BaseResource;
import org.zstack.xinfini.sdk.BaseSpec;
import org.zstack.xinfini.sdk.BaseStatus;

/**
 * @ Author : yh.w
 * @ Date   : Created in 11:29 2024/5/28
 */
public class NodeModule extends BaseResource {
    public NodeModule(Metadata md, NodeSpec spec, NodeStatus status) {
        this.spec = spec;
        this.status = status;
        this.setMetadata(md);
    }

    private NodeSpec spec;
    private NodeStatus status;

    public NodeSpec getSpec() {
        return spec;
    }

    public void setSpec(NodeSpec spec) {
        this.spec = spec;
    }

    public NodeStatus getStatus() {
        return status;
    }

    public void setStatus(NodeStatus status) {
        this.status = status;
    }

    public static class NodeStatus extends BaseStatus {
        private String systemUuid;
        private String vendor;
        private String model;
        private String serial;
        private String arch;
        private String hostname;
        private boolean roleAfaAgent;
        private boolean roleAfaAdmin;
        private boolean roleAfaServer;
        private int sdsHostId;
        private int sdsPlacementNodeId;
        private boolean up;
        private int cpuNum;
        private long memoryMb;
        private int physicalCpuNum;
        private String cpuModel;
        private String bootAt;
        private boolean iommuEnabled;
        private boolean numaEnabled;
        private int poolId;

        public String getSystemUuid() {
            return systemUuid;
        }

        public void setSystemUuid(String systemUuid) {
            this.systemUuid = systemUuid;
        }

        public String getVendor() {
            return vendor;
        }

        public void setVendor(String vendor) {
            this.vendor = vendor;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getSerial() {
            return serial;
        }

        public void setSerial(String serial) {
            this.serial = serial;
        }

        public String getArch() {
            return arch;
        }

        public void setArch(String arch) {
            this.arch = arch;
        }

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public boolean isRoleAfaAgent() {
            return roleAfaAgent;
        }

        public void setRoleAfaAgent(boolean roleAfaAgent) {
            this.roleAfaAgent = roleAfaAgent;
        }

        public boolean isRoleAfaAdmin() {
            return roleAfaAdmin;
        }

        public void setRoleAfaAdmin(boolean roleAfaAdmin) {
            this.roleAfaAdmin = roleAfaAdmin;
        }

        public boolean isRoleAfaServer() {
            return roleAfaServer;
        }

        public void setRoleAfaServer(boolean roleAfaServer) {
            this.roleAfaServer = roleAfaServer;
        }

        public int getSdsHostId() {
            return sdsHostId;
        }

        public void setSdsHostId(int sdsHostId) {
            this.sdsHostId = sdsHostId;
        }

        public int getSdsPlacementNodeId() {
            return sdsPlacementNodeId;
        }

        public void setSdsPlacementNodeId(int sdsPlacementNodeId) {
            this.sdsPlacementNodeId = sdsPlacementNodeId;
        }

        public boolean isUp() {
            return up;
        }

        public void setUp(boolean up) {
            this.up = up;
        }

        public int getCpuNum() {
            return cpuNum;
        }

        public void setCpuNum(int cpuNum) {
            this.cpuNum = cpuNum;
        }

        public long getMemoryMb() {
            return memoryMb;
        }

        public void setMemoryMb(long memoryMb) {
            this.memoryMb = memoryMb;
        }

        public int getPhysicalCpuNum() {
            return physicalCpuNum;
        }

        public void setPhysicalCpuNum(int physicalCpuNum) {
            this.physicalCpuNum = physicalCpuNum;
        }

        public String getCpuModel() {
            return cpuModel;
        }

        public void setCpuModel(String cpuModel) {
            this.cpuModel = cpuModel;
        }

        public String getBootAt() {
            return bootAt;
        }

        public void setBootAt(String bootAt) {
            this.bootAt = bootAt;
        }

        public boolean isIommuEnabled() {
            return iommuEnabled;
        }

        public void setIommuEnabled(boolean iommuEnabled) {
            this.iommuEnabled = iommuEnabled;
        }

        public boolean isNumaEnabled() {
            return numaEnabled;
        }

        public void setNumaEnabled(boolean numaEnabled) {
            this.numaEnabled = numaEnabled;
        }

        public int getPoolId() {
            return poolId;
        }

        public void setPoolId(int poolId) {
            this.poolId = poolId;
        }
    }

    public static class NodeSpec extends BaseSpec {
        private String adminIp;
        private int sddcNodeId;
        private boolean roleAfaAdmin;
        private boolean roleAfaServer;
        private String storagePublicIp;
        private String storagePrivateIp;
        private boolean unrecoverable;

        public String getAdminIp() {
            return adminIp;
        }

        public void setAdminIp(String adminIp) {
            this.adminIp = adminIp;
        }

        public int getSddcNodeId() {
            return sddcNodeId;
        }

        public void setSddcNodeId(int sddcNodeId) {
            this.sddcNodeId = sddcNodeId;
        }

        public boolean isRoleAfaAdmin() {
            return roleAfaAdmin;
        }

        public void setRoleAfaAdmin(boolean roleAfaAdmin) {
            this.roleAfaAdmin = roleAfaAdmin;
        }

        public boolean isRoleAfaServer() {
            return roleAfaServer;
        }

        public void setRoleAfaServer(boolean roleAfaServer) {
            this.roleAfaServer = roleAfaServer;
        }

        public String getStoragePublicIp() {
            return storagePublicIp;
        }

        public void setStoragePublicIp(String storagePublicIp) {
            this.storagePublicIp = storagePublicIp;
        }

        public String getStoragePrivateIp() {
            return storagePrivateIp;
        }

        public void setStoragePrivateIp(String storagePrivateIp) {
            this.storagePrivateIp = storagePrivateIp;
        }

        public boolean isUnrecoverable() {
            return unrecoverable;
        }

        public void setUnrecoverable(boolean unrecoverable) {
            this.unrecoverable = unrecoverable;
        }
    }


}
