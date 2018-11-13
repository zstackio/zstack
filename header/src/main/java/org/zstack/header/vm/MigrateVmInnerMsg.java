package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by camile on 3/7/2018.
 * copy by APIMigrateVmMsg for LongJob
 */
public class MigrateVmInnerMsg extends NeedReplyMessage implements VmInstanceMessage, MigrateVmMessage {
    private String vmInstanceUuid;
    private String hostUuid;
    private Boolean migrateFromDestination;

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public String getVmUuid() {
        return vmInstanceUuid;
    }

    public void setVmUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    @Override
    public boolean isMigrateFromDestination() {
        return migrateFromDestination == null ? false : migrateFromDestination;
    }

    public void setMigrateFromDestination(Boolean migrateFromDestination) {
        this.migrateFromDestination = migrateFromDestination;
    }
}
