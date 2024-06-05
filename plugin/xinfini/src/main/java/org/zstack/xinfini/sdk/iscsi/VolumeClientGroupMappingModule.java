package org.zstack.xinfini.sdk.iscsi;

import org.zstack.xinfini.sdk.BaseResource;
import org.zstack.xinfini.sdk.BaseSpec;
import org.zstack.xinfini.sdk.BaseStatus;

/**
 * @ Author : yh.w
 * @ Date   : Created in 11:29 2024/5/28
 */
public class VolumeClientGroupMappingModule extends BaseResource {
    public VolumeClientGroupMappingModule(Metadata md, VolumeClientGroupMappingSpec spec, VolumeClientGroupMappingStatus status) {
        this.spec = spec;
        this.status = status;
        this.setMetadata(md);
    }

    private VolumeClientGroupMappingSpec spec;
    private VolumeClientGroupMappingStatus status;

    public VolumeClientGroupMappingSpec getSpec() {
        return spec;
    }

    public void setSpec(VolumeClientGroupMappingSpec spec) {
        this.spec = spec;
    }

    public VolumeClientGroupMappingStatus getStatus() {
        return status;
    }

    public void setStatus(VolumeClientGroupMappingStatus status) {
        this.status = status;
    }

    public static class VolumeClientGroupMappingStatus extends BaseStatus {

    }

    public static class VolumeClientGroupMappingSpec extends BaseSpec {
        private Integer bsVolumeId;
        private Integer iscsiClientGroupId;
        private Integer lunId;

        public Integer getBsVolumeId() {
            return bsVolumeId;
        }

        public void setBsVolumeId(Integer bsVolumeId) {
            this.bsVolumeId = bsVolumeId;
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
    }


}
