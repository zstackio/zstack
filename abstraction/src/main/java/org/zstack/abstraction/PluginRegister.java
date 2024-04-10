package org.zstack.abstraction;

import java.util.Map;

/**
 * PluginRegister defines the specific requirements for plugin.
 * <p>
 *     by PluginRegister we should get all metadata of the plugin.
 * </p>
 */
public interface PluginRegister {
    /**
     * product name of your plugin
     * @return a String of name
     */
    String productName();

    /**
     * unique product key from distribution
     * @return the key of current environment
     */
    String productKey();

    /**
     * plugin version
     * @return version of current plugin
     */
    String version();

    /**
     * capabilities map describe current plugin's whole
     * capabilities
     * @return map of capabilities
     */
    Map<String, PluginCapabilityState> capabilities();
}
