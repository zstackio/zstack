package org.zstack.kvm;

/**
 */
public class KVMHostConnectedContext extends KVMHostContext {
    private boolean newAddedHost;

    public boolean isNewAddedHost() {
        return newAddedHost;
    }

    public void setNewAddedHost(boolean newAddedHost) {
        this.newAddedHost = newAddedHost;
    }

    public KVMHostConnectedContext(KVMHostContext context, boolean newHost) {
        newAddedHost = newHost;
        setBaseUrl(context.getBaseUrl());
        setInventory(context.getInventory());
    }
}
