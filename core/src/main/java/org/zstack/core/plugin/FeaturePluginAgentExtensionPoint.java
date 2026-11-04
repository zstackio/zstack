package org.zstack.core.plugin;

import java.io.File;
import java.util.Collections;
import java.util.Map;

/**
 * Implemented by a hypervisor module that deploys the agent parts of feature plugins to its hosts.
 */
public interface FeaturePluginAgentExtensionPoint {
    /**
     * The agent parts of feature plugins on this management node changed while it keeps running: deploy them to
     * the hosts it manages now. A management node that restarts instead finds its agent module changed on boot
     * and reconnects its hosts then.
     */
    void agentPartsChanged();

    /**
     * A plugin package was uploaded: install the system packages its agent part depends on, in the background, so
     * that installing the plugin later does not wait for them.
     */
    default void packageAdded(String pluginId, String sha256, File pkg) {
    }

    /**
     * @return plugin id -&gt; number of hosts the agent part of the plugin is still being deployed to
     */
    default Map<String, Integer> deployingHosts() {
        return Collections.emptyMap();
    }
}
