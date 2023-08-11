package org.zstack.core.plugin;

import org.zstack.abstraction.PluginDriver;

import java.util.List;

/**
 * PluginManager interface for plugin related operations.
 * <p>
 *     isFeatureSupported used for plugin capability check.
 *     getPlugin used for get plugin singleton.
 * </p>
 */
public interface PluginManager {
    boolean isFeatureSupported(String pluginUuid, String capability);

    <T extends PluginDriver> T getPlugin(String pluginUuid);

    <T extends PluginDriver> List<T> getPluginList(Class<? extends PluginDriver> pluginClass);

    // check if a sub plugin class with a type exists
    boolean isPluginTypeExist(Class<? extends PluginDriver> pluginClass, String type);

    // get plugin class with type
    <T extends PluginDriver> T getPlugin(Class<? extends PluginDriver> pluginClass, String type);
}
