package org.zstack.kvm;

import java.util.Map;

/**
 */
public class KVMHostConnectedContext {
    private KVMHostInventory inventory;
    private boolean newAddedHost;
    private String baseUrl;
    private String skipPackages;
    private Map<String, Object> pluginFacts;
    private Map<String, Map<String, String>> featurePlugins;

    public Map<String, Object> getPluginFacts() {
        return pluginFacts;
    }

    public void setPluginFacts(Map<String, Object> pluginFacts) {
        this.pluginFacts = pluginFacts;
    }

    public Map<String, Map<String, String>> getFeaturePlugins() {
        return featurePlugins;
    }

    public void setFeaturePlugins(Map<String, Map<String, String>> featurePlugins) {
        this.featurePlugins = featurePlugins;
    }

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
}
