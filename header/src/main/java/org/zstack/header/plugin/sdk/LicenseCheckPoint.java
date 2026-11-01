package org.zstack.header.plugin.sdk;

/**
 * When the platform checks the license of a feature plugin.
 */
@PluginSdk(since = "1.0")
public enum LicenseCheckPoint {
    /**
     * before installing a plugin through the API
     */
    INSTALL,
    /**
     * when a management node starts with the plugin loaded
     */
    LOAD
}
