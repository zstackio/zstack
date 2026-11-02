package org.zstack.core.plugin;

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
}
