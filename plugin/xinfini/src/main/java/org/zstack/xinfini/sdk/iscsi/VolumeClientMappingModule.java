package org.zstack.xinfini.sdk.iscsi;

import org.zstack.xinfini.sdk.BaseResource;
import org.zstack.xinfini.sdk.BaseSpec;
import org.zstack.xinfini.sdk.BaseStatus;

import java.util.List;

/**
 * @ Author : yh.w
 * @ Date   : Created in 11:29 2024/5/28
 */
public class VolumeClientMappingModule extends BaseResource {
    public VolumeClientMappingModule(Metadata md, VolumeClientMappingSpec spec, VolumeClientMappingStatus status) {
        this.spec = spec;
        this.status = status;
        this.setMetadata(md);
    }

    private VolumeClientMappingSpec spec;
    private VolumeClientMappingStatus status;

    public VolumeClientMappingSpec getSpec() {
        return spec;
    }

    public void setSpec(VolumeClientMappingSpec spec) {
        this.spec = spec;
    }

    public VolumeClientMappingStatus getStatus() {
        return status;
    }

    public void setStatus(VolumeClientMappingStatus status) {
        this.status = status;
    }

    public static class VolumeClientMappingStatus extends BaseStatus {
        private Integer pathNum;
        private List<Integer> iscsiTargetIds;
        private List<Integer> iscsiTargetGroupIds;

        public Integer getPathNum() {
            return pathNum;
        }

        public void setPathNum(Integer pathNum) {
            this.pathNum = pathNum;
        }

        public List<Integer> getIscsiTargetIds() {
            return iscsiTargetIds;
        }

        public void setIscsiTargetIds(List<Integer> iscsiTargetIds) {
            this.iscsiTargetIds = iscsiTargetIds;
        }

        public List<Integer> getIscsiTargetGroupIds() {
            return iscsiTargetGroupIds;
        }

        public void setIscsiTargetGroupIds(List<Integer> iscsiTargetGroupIds) {
            this.iscsiTargetGroupIds = iscsiTargetGroupIds;
        }
    }

    public static class VolumeClientMappingSpec extends BaseSpec {
        private Integer bsVolumeId;
        private String protocol;
        private Integer pathNum;
        private Integer nvmeClientId;
        private Integer nsId;
        private Integer iscsiClientId;
        private Integer iscsiClientGroupId;
        private Integer lunId;
        private List<Integer> iscsiTargetIds;
        private List<Integer> iscsiTargetGroupIds;

        public Integer getBsVolumeId() {
            return bsVolumeId;
        }

        public void setBsVolumeId(Integer bsVolumeId) {
            this.bsVolumeId = bsVolumeId;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public Integer getPathNum() {
            return pathNum;
        }

        public void setPathNum(Integer pathNum) {
            this.pathNum = pathNum;
        }

        public Integer getNvmeClientId() {
            return nvmeClientId;
        }

        public void setNvmeClientId(Integer nvmeClientId) {
            this.nvmeClientId = nvmeClientId;
        }

        public Integer getNsId() {
            return nsId;
        }

        public void setNsId(Integer nsId) {
            this.nsId = nsId;
        }

        public Integer getIscsiClientId() {
            return iscsiClientId;
        }

        public void setIscsiClientId(Integer iscsiClientId) {
            this.iscsiClientId = iscsiClientId;
        }

        public Integer getIscsiClientGroupId() {
            return iscsiClientGroupId;
        }

        public void setIscsiClientGroupId(Integer iscsiClientGroupId) {
            this.iscsiClientGroupId = iscsiClientGroupId;
        }

        public Integer getLunId() {
            return lunId;
        }

        public void setLunId(Integer lunId) {
            this.lunId = lunId;
        }

        public List<Integer> getIscsiTargetIds() {
            return iscsiTargetIds;
        }

        public void setIscsiTargetIds(List<Integer> iscsiTargetIds) {
            this.iscsiTargetIds = iscsiTargetIds;
        }

        public List<Integer> getIscsiTargetGroupIds() {
            return iscsiTargetGroupIds;
        }

        public void setIscsiTargetGroupIds(List<Integer> iscsiTargetGroupIds) {
            this.iscsiTargetGroupIds = iscsiTargetGroupIds;
        }
    }


}
