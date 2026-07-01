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
    String SERVICE_ID = "external.plugin";

    boolean isFeatureSupported(String pluginUuid, String capability);

    <T extends PluginDriver> T getPlugin(String pluginUuid);

    <T extends PluginDriver> List<T> getPluginList(Class<? extends PluginDriver> pluginClass);

    // check if a sub plugin class with a type exists
    boolean isPluginTypeExist(Class<? extends PluginDriver> pluginClass, String type);

    // get plugin class with type
    <T extends PluginDriver> T getPlugin(Class<? extends PluginDriver> pluginClass, String type);

    /**
     * Create a new independent instance of a plugin driver.
     * Unlike getPlugin() which returns a shared singleton, this method creates
     * a fresh instance each time to avoid concurrent state corruption when
     * multiple callers need isolated driver configurations.
     *
     * @param pluginUuid the plugin product key / UUID
     * @return a new instance of the plugin driver
     */
    <T extends PluginDriver> T newDriverInstance(String pluginUuid);
}
