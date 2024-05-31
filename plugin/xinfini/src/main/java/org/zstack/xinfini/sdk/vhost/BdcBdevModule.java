package org.zstack.xinfini.sdk.vhost;

import org.zstack.xinfini.sdk.BaseResource;
import org.zstack.xinfini.sdk.BaseSpec;
import org.zstack.xinfini.sdk.BaseStatus;

import java.util.List;

/**
 * @ Author : yh.w
 * @ Date   : Created in 11:29 2024/5/28
 */
public class BdcBdevModule extends BaseResource {
    public BdcBdevModule(Metadata md, BdcBdevModule.BdcBdevSpec spec, BdcBdevModule.BdcBdevStatus status) {
        this.spec = spec;
        this.status = status;
        this.setMetadata(md);
    }

    private BdcBdevModule.BdcBdevSpec spec;
    private BdcBdevModule.BdcBdevStatus status;

    public BdcBdevSpec getSpec() {
        return spec;
    }

    public void setSpec(BdcBdevSpec spec) {
        this.spec = spec;
    }

    public BdcBdevStatus getStatus() {
        return status;
    }

    public void setStatus(BdcBdevStatus status) {
        this.status = status;
    }

    public static class BdcBdevStatus extends BaseStatus{
        private Integer[] cpuIds;

        public Integer[] getCpuIds() {
            return cpuIds;
        }

        public void setCpuIds(Integer[] cpuIds) {
            this.cpuIds = cpuIds;
        }
    }

    public static class BdcBdevSpec extends BaseSpec{
        private int bdcId;
        private String nodeIp;
        private int bsVolumeId;
        private Integer[] numaNodeIds;
        private String socketPath;
        private int queueNum;
        private String bsVolumeName;
        private String bsVolumeUuid;

        public int getBdcId() {
            return bdcId;
        }

        public void setBdcId(int bdcId) {
            this.bdcId = bdcId;
        }

        public String getNodeIp() {
            return nodeIp;
        }

        public void setNodeIp(String nodeIp) {
            this.nodeIp = nodeIp;
        }

        public int getBsVolumeId() {
            return bsVolumeId;
        }

        public void setBsVolumeId(int bsVolumeId) {
            this.bsVolumeId = bsVolumeId;
        }

        public Integer[] getNumaNodeIds() {
            return numaNodeIds;
        }

        public void setNumaNodeIds(Integer[] numaNodeIds) {
            this.numaNodeIds = numaNodeIds;
        }

        public String getSocketPath() {
            return socketPath;
        }

        public void setSocketPath(String socketPath) {
            this.socketPath = socketPath;
        }

        public int getQueueNum() {
            return queueNum;
        }

        public void setQueueNum(int queueNum) {
            this.queueNum = queueNum;
        }

        public String getBsVolumeName() {
            return bsVolumeName;
        }

        public void setBsVolumeName(String bsVolumeName) {
            this.bsVolumeName = bsVolumeName;
        }

        public String getBsVolumeUuid() {
            return bsVolumeUuid;
        }

        public void setBsVolumeUuid(String bsVolumeUuid) {
            this.bsVolumeUuid = bsVolumeUuid;
        }
    }
}
