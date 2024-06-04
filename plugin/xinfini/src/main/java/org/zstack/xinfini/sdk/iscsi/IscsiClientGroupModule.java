package org.zstack.xinfini.sdk.iscsi;

import org.zstack.xinfini.sdk.BaseResource;
import org.zstack.xinfini.sdk.BaseSpec;
import org.zstack.xinfini.sdk.BaseStatus;

import java.util.List;

/**
 * @ Author : yh.w
 * @ Date   : Created in 11:29 2024/5/28
 */
public class IscsiClientGroupModule extends BaseResource {

    private IscsiClientGroupSpec spec;

    private IscsiClientGroupStatus status;

    public IscsiClientGroupModule(Metadata md, IscsiClientGroupSpec spec, IscsiClientGroupStatus status) {
        this.spec = spec;
        this.status = status;
        this.setMetadata(md);
    }

    public IscsiClientGroupSpec getSpec() {
        return spec;
    }

    public void setSpec(IscsiClientGroupSpec spec) {
        this.spec = spec;
    }

    public IscsiClientGroupStatus getStatus() {
        return status;
    }

    public void setStatus(IscsiClientGroupStatus status) {
        this.status = status;
    }

    public static class IscsiClientGroupSpec extends BaseSpec {
        private Integer gatewayNumPerVolume;
        private Integer targetGroupNum;
        private Integer targetNumPerVolume;
        private String chapState;

        public Integer getGatewayNumPerVolume() {
            return gatewayNumPerVolume;
        }

        public void setGatewayNumPerVolume(Integer gatewayNumPerVolume) {
            this.gatewayNumPerVolume = gatewayNumPerVolume;
        }

        public Integer getTargetGroupNum() {
            return targetGroupNum;
        }

        public void setTargetGroupNum(Integer targetGroupNum) {
            this.targetGroupNum = targetGroupNum;
        }

        public Integer getTargetNumPerVolume() {
            return targetNumPerVolume;
        }

        public void setTargetNumPerVolume(Integer targetNumPerVolume) {
            this.targetNumPerVolume = targetNumPerVolume;
        }

        public String getChapState() {
            return chapState;
        }

        public void setChapState(String chapState) {
            this.chapState = chapState;
        }
    }

    public static class IscsiClientGroupStatus extends BaseStatus {
        private Integer iscsiGatewayNum;
        private Integer targetGroupNum;
        private Integer targetNumPerVolume;
        private String chapState;
        private Integer bsVolumeNum;
        private Integer iscsiClientNum;

        public Integer getIscsiGatewayNum() {
            return iscsiGatewayNum;
        }

        public void setIscsiGatewayNum(Integer iscsiGatewayNum) {
            this.iscsiGatewayNum = iscsiGatewayNum;
        }

        public Integer getTargetGroupNum() {
            return targetGroupNum;
        }

        public void setTargetGroupNum(Integer targetGroupNum) {
            this.targetGroupNum = targetGroupNum;
        }

        public Integer getTargetNumPerVolume() {
            return targetNumPerVolume;
        }

        public void setTargetNumPerVolume(Integer targetNumPerVolume) {
            this.targetNumPerVolume = targetNumPerVolume;
        }

        public String getChapState() {
            return chapState;
        }

        public void setChapState(String chapState) {
            this.chapState = chapState;
        }

        public Integer getBsVolumeNum() {
            return bsVolumeNum;
        }

        public void setBsVolumeNum(Integer bsVolumeNum) {
            this.bsVolumeNum = bsVolumeNum;
        }

        public Integer getIscsiClientNum() {
            return iscsiClientNum;
        }

        public void setIscsiClientNum(Integer iscsiClientNum) {
            this.iscsiClientNum = iscsiClientNum;
        }
    }

}
