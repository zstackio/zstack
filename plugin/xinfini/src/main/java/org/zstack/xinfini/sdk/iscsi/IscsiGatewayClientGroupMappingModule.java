package org.zstack.xinfini.sdk.iscsi;

import org.zstack.xinfini.sdk.BaseResource;
import org.zstack.xinfini.sdk.BaseSpec;
import org.zstack.xinfini.sdk.BaseStatus;

/**
 * @ Author : yh.w
 * @ Date   : Created in 11:29 2024/5/28
 */
public class IscsiGatewayClientGroupMappingModule extends BaseResource {
    public IscsiGatewayClientGroupMappingModule(Metadata md, IscsiGatewayClientGroupMappingSpec spec, IscsiGatewayClientGroupMappingStatus status) {
        this.spec = spec;
        this.status = status;
        this.setMetadata(md);
    }

    private IscsiGatewayClientGroupMappingSpec spec;
    private IscsiGatewayClientGroupMappingStatus status;

    public IscsiGatewayClientGroupMappingSpec getSpec() {
        return spec;
    }

    public void setSpec(IscsiGatewayClientGroupMappingSpec spec) {
        this.spec = spec;
    }

    public IscsiGatewayClientGroupMappingStatus getStatus() {
        return status;
    }

    public void setStatus(IscsiGatewayClientGroupMappingStatus status) {
        this.status = status;
    }

    public static class IscsiGatewayClientGroupMappingStatus extends BaseStatus {
        private int iscsiPathNum;

        public int getIscsiPathNum() {
            return iscsiPathNum;
        }

        public void setIscsiPathNum(int iscsiPathNum) {
            this.iscsiPathNum = iscsiPathNum;
        }
    }

    public static class IscsiGatewayClientGroupMappingSpec extends BaseSpec {

        private Integer iscsiGatewayId;

        private Integer iscsiClientGroupId;

        public Integer getIscsiClientGroupId() {
            return iscsiClientGroupId;
        }

        public void setIscsiClientGroupId(Integer iscsiClientGroupId) {
            this.iscsiClientGroupId = iscsiClientGroupId;
        }

        public Integer getIscsiGatewayId() {
            return iscsiGatewayId;
        }

        public void setIscsiGatewayId(Integer iscsiGatewayId) {
            this.iscsiGatewayId = iscsiGatewayId;
        }
    }


}
