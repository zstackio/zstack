package org.zstack.header.vm;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;

public class VmVsocMigrateMsg extends NeedReplyMessage implements HostMessage {
    private String destSocId;
    private String srcHostIp;
    private String vmUuid;
    private String migrateType;
    private String uuid;

    public String getDestSocId() {
        return destSocId;
    }

    public void setDestSocId(String destSocId) {
        this.destSocId = destSocId;
    }

    public String getSrcHostIp() {
        return srcHostIp;
    }

    public void setSrcHostIp(String srcHostIp) {
        this.srcHostIp = srcHostIp;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public String getMigrateType() {
        return migrateType;
    }

    public void setMigrateType(String migrateType) {
        this.migrateType = migrateType;
    }

    @Override
    public String getHostUuid() {
        return getUuid();
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
