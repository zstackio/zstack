package org.zstack.header.vm;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;

public class VmUpdateNicOnHypervisorMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;
    private String vmInstanceUuid;
    private boolean migration;

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public boolean isMigration() {
        return migration;
    }

    public void setMigration(boolean migration) {
        this.migration = migration;
    }
}
