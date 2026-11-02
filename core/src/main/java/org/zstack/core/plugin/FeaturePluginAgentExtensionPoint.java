package org.zstack.core.plugin;

/**
 * Implemented by a hypervisor module that deploys the agent parts of feature plugins to its hosts.
 */
public interface FeaturePluginAgentExtensionPoint {
    /**
     * The agent parts of feature plugins on this management node changed.
     *
     * @param reconnectHosts reconnect the hosts this management node manages so that they get the change;
     *                       false when this management node restarts and reconnects them on boot
     */
    void agentPartsChanged(boolean reconnectHosts);
}
