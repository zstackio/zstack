package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

/**
 */
public class MigrateVmMsg extends NeedReplyMessage implements VmInstanceMessage, MigrateVmMessage {
    private String vmInstanceUuid;
    private List<String> avoidHostUuids;

    public List<String> getAvoidHostUuids() {
        return avoidHostUuids;
    }

    public void setAvoidHostUuids(List<String> avoidHostUuids) {
        this.avoidHostUuids = avoidHostUuids;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    @Override
    public String getHostUuid() {
        return null;
    }

    @Override
    public boolean isMigrateFromDestination() {
        return false;
    }
}
