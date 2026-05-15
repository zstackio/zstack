package org.zstack.header.vm;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;

public class StopVmOnHypervisorMsg extends NeedReplyMessage implements HostMessage {
    private VmInstanceInventory vmInventory;
    private String type;
    private boolean debug;
    private boolean ignoreNotFound;

    public VmInstanceInventory getVmInventory() {
        return vmInventory;
    }

    public void setVmInventory(VmInstanceInventory vmInventory) {
        this.vmInventory = vmInventory;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getHostUuid() {
        return vmInventory.getHostUuid();
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean isIgnoreNotFound() {
        return ignoreNotFound;
    }

    public void setIgnoreNotFound(boolean ignoreNotFound) {
        this.ignoreNotFound = ignoreNotFound;
    }
}
