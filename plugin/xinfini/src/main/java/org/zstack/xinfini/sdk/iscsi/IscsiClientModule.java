package org.zstack.xinfini.sdk.iscsi;

import org.zstack.xinfini.sdk.BaseResource;
import org.zstack.xinfini.sdk.BaseSpec;
import org.zstack.xinfini.sdk.BaseStatus;

import java.util.List;

/**
 * @ Author : yh.w
 * @ Date   : Created in 11:29 2024/5/28
 */
public class IscsiClientModule extends BaseResource {

    private IscsiClientSpec spec;

    private IscsiClientStatus status;

    public IscsiClientModule(Metadata md, IscsiClientSpec spec, IscsiClientStatus status) {
        this.spec = spec;
        this.status = status;
        this.setMetadata(md);
    }

    public IscsiClientSpec getSpec() {
        return spec;
    }

    public void setSpec(IscsiClientSpec spec) {
        this.spec = spec;
    }

    public IscsiClientStatus getStatus() {
        return status;
    }

    public void setStatus(IscsiClientStatus status) {
        this.status = status;
    }

    public static class IscsiClientSpec extends BaseSpec {
        private Integer iscsiClientGroupId;
        private String code;
        private Integer gatewayNumPerVolume;
        private Integer targetGroupNum;
        private Integer targetNumPerVolume;
        private String chapState;

        public Integer getIscsiClientGroupId() {
            return iscsiClientGroupId;
        }

        public void setIscsiClientGroupId(Integer iscsiClientGroupId) {
            this.iscsiClientGroupId = iscsiClientGroupId;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

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

    public static class IscsiClientStatus extends BaseStatus {
        private List<String> targetIqns;
        private Integer iscsiGatewayNum;
        private Integer targetGroupNum;
        private Integer targetNumPerVolume;
        private String chapState;
        private Integer bsVolumeNum;

        public List<String> getTargetIqns() {
            return targetIqns;
        }

        public void setTargetIqns(List<String> targetIqns) {
            this.targetIqns = targetIqns;
        }

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
    }

}
