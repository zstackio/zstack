package org.zstack.kvm;

import java.util.Set;

/**
 */
public class KVMHostConnectedContext {
    private KVMHostInventory inventory;
    private boolean newAddedHost;
    private String baseUrl;
    private String skipPackages;
    private Set<String> attachedPrimaryStorageTypes;

    public boolean isNewAddedHost() {
        return newAddedHost;
    }

    public void setNewAddedHost(boolean newAddedHost) {
        this.newAddedHost = newAddedHost;
    }

    public KVMHostInventory getInventory() {
        return inventory;
    }

    public void setInventory(KVMHostInventory inventory) {
        this.inventory = inventory;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getSkipPackages() {
        return skipPackages;
    }

    public void setSkipPackages(String skipPackages) {
        this.skipPackages = skipPackages;
    }

    public Set<String> getAttachedPrimaryStorageTypes() {
        return attachedPrimaryStorageTypes;
    }

    public void setAttachedPrimaryStorageTypes(Set<String> attachedPrimaryStorageTypes) {
        this.attachedPrimaryStorageTypes = attachedPrimaryStorageTypes;
    }
}
