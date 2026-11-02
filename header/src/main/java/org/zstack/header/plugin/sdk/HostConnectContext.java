package org.zstack.header.plugin.sdk;

import org.zstack.header.host.HostInventory;

import java.util.Map;

/**
 * What a {@link HostConnectHook} sees of a connecting host.
 */
@PluginSdk(since = "1.0")
public class HostConnectContext {
    private final HostInventory inventory;
    private final boolean newAdded;
    private final Map<String, Object> pluginFacts;
    private final Map<String, Map<String, String>> agentPlugins;

    public HostConnectContext(HostInventory inventory, boolean newAdded, Map<String, Object> pluginFacts,
                              Map<String, Map<String, String>> agentPlugins) {
        this.inventory = inventory;
        this.newAdded = newAdded;
        this.pluginFacts = pluginFacts;
        this.agentPlugins = agentPlugins;
    }

    public String hostUuid() {
        return inventory.getUuid();
    }

    public HostInventory inventory() {
        return inventory;
    }

    public boolean isNewAdded() {
        return newAdded;
    }

    /**
     * @return what the plugin's host fact provider on the host reported, {"error": ...} if it failed,
     * null if the plugin reported nothing
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> pluginFacts(String pluginId) {
        return pluginFacts == null ? null : (Map<String, Object>) pluginFacts.get(pluginId);
    }

    /**
     * @return status, error and version of the plugin's kvmagent part on the host, null if it is not there
     */
    public Map<String, String> agentPluginStatus(String pluginId) {
        return agentPlugins == null ? null : agentPlugins.get(pluginId);
    }
}
