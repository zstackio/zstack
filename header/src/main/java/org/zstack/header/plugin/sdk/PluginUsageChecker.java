package org.zstack.header.plugin.sdk;

import java.util.List;

/**
 * Implemented by a feature plugin to tell whether it is still in use. A plugin in use cannot be uninstalled.
 * Without an implementation, the tables listed in lifecycle.inUse of plugin.yaml decide.
 */
@PluginSdk(since = "1.0")
public interface PluginUsageChecker {
    /**
     * @return why the plugin is in use, shown to the user as they are, e.g. "3 VMs have vTPM enabled";
     * empty if the plugin is not in use. Throwing an exception counts as in use.
     */
    List<String> inUseReasons();
}
