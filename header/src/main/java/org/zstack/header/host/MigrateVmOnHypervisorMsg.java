package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.VmInstanceInventory;

public class MigrateVmOnHypervisorMsg extends NeedReplyMessage implements HostMessage {
    private VmInstanceInventory vmInventory;
    private HostInventory destHostInventory;
    private String srcHostUuid;
    private boolean withStorage;

    public boolean isWithStorage() {
        return withStorage;
    }

    public void setWithStorage(boolean withStorage) {
        this.withStorage = withStorage;
    }

    public VmInstanceInventory getVmInventory() {
        return vmInventory;
    }
    public void setVmInventory(VmInstanceInventory vmInventory) {
        this.vmInventory = vmInventory;
    }
    public HostInventory getDestHostInventory() {
        return destHostInventory;
    }
    public void setDestHostInventory(HostInventory destHostInventory) {
        this.destHostInventory = destHostInventory;
    }
    @Override
    public String getHostUuid() {
        return getSrcHostUuid();
    }
    public String getSrcHostUuid() {
        return srcHostUuid;
    }
    public void setSrcHostUuid(String srcHostUuid) {
        this.srcHostUuid = srcHostUuid;
    }
}
