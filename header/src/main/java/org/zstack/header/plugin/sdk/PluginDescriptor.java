package org.zstack.header.plugin.sdk;

/**
 * What a license check knows about a feature plugin, from its plugin.yaml.
 */
@PluginSdk(since = "1.0")
public class PluginDescriptor {
    private final String id;
    private final String version;
    private final String licenseAddon;

    public PluginDescriptor(String id, String version, String licenseAddon) {
        this.id = id;
        this.version = version;
        this.licenseAddon = licenseAddon;
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    /**
     * @return license.addon of plugin.yaml, null if the plugin does not declare one
     */
    public String getLicenseAddon() {
        return licenseAddon;
    }
}
