package org.zstack.xinfini.sdk.iscsi;

import org.zstack.xinfini.sdk.BaseResource;
import org.zstack.xinfini.sdk.BaseSpec;
import org.zstack.xinfini.sdk.BaseStatus;

import java.util.List;

/**
 * @ Author : yh.w
 * @ Date   : Created in 11:29 2024/5/28
 */
public class GatewayClientGroupMappingModule extends BaseResource {
    public GatewayClientGroupMappingModule(Metadata md, GatewayClientGroupMappingSpec spec, GatewayClientGroupMappingStatus status) {
        this.spec = spec;
        this.status = status;
        this.setMetadata(md);
    }

    private GatewayClientGroupMappingSpec spec;
    private GatewayClientGroupMappingStatus status;

    public GatewayClientGroupMappingSpec getSpec() {
        return spec;
    }

    public void setSpec(GatewayClientGroupMappingSpec spec) {
        this.spec = spec;
    }

    public GatewayClientGroupMappingStatus getStatus() {
        return status;
    }

    public void setStatus(GatewayClientGroupMappingStatus status) {
        this.status = status;
    }

    public static class GatewayClientGroupMappingStatus extends BaseStatus {
        private int iscsiPathNum;

        public int getIscsiPathNum() {
            return iscsiPathNum;
        }

        public void setIscsiPathNum(int iscsiPathNum) {
            this.iscsiPathNum = iscsiPathNum;
        }
    }

    public static class GatewayClientGroupMappingSpec extends BaseSpec {

        private String iscsiGatewayId;

        private String iscsiClientGroupId;

        public java.lang.String getIscsiGatewayId() {
            return iscsiGatewayId;
        }

        public void setIscsiGatewayId(java.lang.String iscsiGatewayId) {
            this.iscsiGatewayId = iscsiGatewayId;
        }

        public String getIscsiClientGroupId() {
            return iscsiClientGroupId;
        }

        public void setIscsiClientGroupId(String iscsiClientGroupId) {
            this.iscsiClientGroupId = iscsiClientGroupId;
        }
    }


}
