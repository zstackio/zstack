package org.zstack.sdk.sns.platform.plugin;

import org.zstack.sdk.PluginDriverInventory;

public class SNSPluginPlatformInventory extends org.zstack.sdk.sns.SNSApplicationPlatformInventory {

    public java.lang.String pluginDriverUuid;
    public void setPluginDriverUuid(java.lang.String pluginDriverUuid) {
        this.pluginDriverUuid = pluginDriverUuid;
    }
    public java.lang.String getPluginDriverUuid() {
        return this.pluginDriverUuid;
    }

    public java.util.Map properties;
    public void setProperties(java.util.Map properties) {
        this.properties = properties;
    }
    public java.util.Map getProperties() {
        return this.properties;
    }

    public PluginDriverInventory driver;
    public void setDriver(PluginDriverInventory driver) {
        this.driver = driver;
    }
    public PluginDriverInventory getDriver() {
        return this.driver;
    }

}
