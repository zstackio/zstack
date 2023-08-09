package org.zstack.core.plugin;

import org.zstack.abstraction.PluginRegister;

import java.util.List;

/**
 * PluginManager interface for plugin related operations.
 * <p>
 *     isCapabilitySupported used for plugin capability check.
 *     getPlugin used for get plugin singleton.
 * </p>
 */
public interface PluginManager {
    boolean isCapabilitySupported(String pluginProductKey, String capability);

    <T extends PluginRegister> T getPlugin(String pluginProductKey);

    <T extends PluginRegister> List<T> getPluginList(Class<? extends PluginRegister> pluginClass);
}
