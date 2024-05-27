package org.zstack.xinfini.sdk.vhost;

import org.zstack.xinfini.sdk.BaseResource;
import org.zstack.xinfini.sdk.BaseSpec;
import org.zstack.xinfini.sdk.BaseStatus;

import java.util.List;

/**
 * @ Author : yh.w
 * @ Date   : Created in 11:29 2024/5/28
 */
public class BdcModule extends BaseResource {
    public BdcModule(BaseResource.Metadata md, BdcModule.BdcSpec spec, BdcModule.BdcStatus status) {
        this.spec = spec;
        this.status = status;
        this.setMetadata(md);
    }

    private BdcModule.BdcSpec spec;
    private BdcModule.BdcStatus status;

    public BdcModule.BdcSpec getSpec() {
        return spec;
    }

    public void setSpec(BdcModule.BdcSpec spec) {
        this.spec = spec;
    }

    public BdcStatus getStatus() {
        return status;
    }

    public void setStatus(BdcStatus status) {
        this.status = status;
    }

    public static class BdcStatus extends BaseStatus{
        private int xbsMetaClusterId;
        private boolean installed;
        private String runState;
        private String version;
        private String systemUuid;
        private String hostname;
        private String osVersion;
        private String vendor;
        private String cpuModel;
        private String cpuArch;
        private int cpuNum;
        private int memoryMb;
        private int volumeNum;
        private int physicalCpuNum;
        private int pid;

        public int getXbsMetaClusterId() {
            return xbsMetaClusterId;
        }

        public void setXbsMetaClusterId(int xbsMetaClusterId) {
            this.xbsMetaClusterId = xbsMetaClusterId;
        }

        public boolean isInstalled() {
            return installed;
        }

        public void setInstalled(boolean installed) {
            this.installed = installed;
        }

        public String getRunState() {
            return runState;
        }

        public void setRunState(String runState) {
            this.runState = runState;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getSystemUuid() {
            return systemUuid;
        }

        public void setSystemUuid(String systemUuid) {
            this.systemUuid = systemUuid;
        }

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public String getOsVersion() {
            return osVersion;
        }

        public void setOsVersion(String osVersion) {
            this.osVersion = osVersion;
        }

        public String getVendor() {
            return vendor;
        }

        public void setVendor(String vendor) {
            this.vendor = vendor;
        }

        public String getCpuModel() {
            return cpuModel;
        }

        public void setCpuModel(String cpuModel) {
            this.cpuModel = cpuModel;
        }

        public String getCpuArch() {
            return cpuArch;
        }

        public void setCpuArch(String cpuArch) {
            this.cpuArch = cpuArch;
        }

        public int getCpuNum() {
            return cpuNum;
        }

        public void setCpuNum(int cpuNum) {
            this.cpuNum = cpuNum;
        }

        public int getMemoryMb() {
            return memoryMb;
        }

        public void setMemoryMb(int memoryMb) {
            this.memoryMb = memoryMb;
        }

        public int getVolumeNum() {
            return volumeNum;
        }

        public void setVolumeNum(int volumeNum) {
            this.volumeNum = volumeNum;
        }

        public int getPhysicalCpuNum() {
            return physicalCpuNum;
        }

        public void setPhysicalCpuNum(int physicalCpuNum) {
            this.physicalCpuNum = physicalCpuNum;
        }

        public int getPid() {
            return pid;
        }

        public void setPid(int pid) {
            this.pid = pid;
        }
    }

    public static class BdcSpec extends BaseSpec{
        private String ip;
        private int port;
        private int adminPort;
        private int metricPort;
        private int apiserverPort;
        private String token;
        private String installStage;
        private int xbsMetaClusterId;
        private List<Integer> cpuIds;
        private int hugepageSizeMb;
        private String transportType;
        private String installCmd;

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public int getAdminPort() {
            return adminPort;
        }

        public void setAdminPort(int adminPort) {
            this.adminPort = adminPort;
        }

        public int getMetricPort() {
            return metricPort;
        }

        public void setMetricPort(int metricPort) {
            this.metricPort = metricPort;
        }

        public int getApiserverPort() {
            return apiserverPort;
        }

        public void setApiserverPort(int apiserverPort) {
            this.apiserverPort = apiserverPort;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getInstallStage() {
            return installStage;
        }

        public void setInstallStage(String installStage) {
            this.installStage = installStage;
        }

        public int getXbsMetaClusterId() {
            return xbsMetaClusterId;
        }

        public void setXbsMetaClusterId(int xbsMetaClusterId) {
            this.xbsMetaClusterId = xbsMetaClusterId;
        }

        public List<Integer> getCpuIds() {
            return cpuIds;
        }

        public void setCpuIds(List<Integer> cpuIds) {
            this.cpuIds = cpuIds;
        }

        public int getHugepageSizeMb() {
            return hugepageSizeMb;
        }

        public void setHugepageSizeMb(int hugepageSizeMb) {
            this.hugepageSizeMb = hugepageSizeMb;
        }

        public String getTransportType() {
            return transportType;
        }

        public void setTransportType(String transportType) {
            this.transportType = transportType;
        }

        public String getInstallCmd() {
            return installCmd;
        }

        public void setInstallCmd(String installCmd) {
            this.installCmd = installCmd;
        }
    }
}
