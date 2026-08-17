package org.zstack.sdk.zwatch.alarm.sns.template.plugin;

import org.zstack.sdk.PluginDriverInventory;

public class SNSPluginTextTemplateInventory extends org.zstack.sdk.zwatch.alarm.sns.SNSTextTemplateInventory {

    public java.lang.String pluginDriverUuid;
    public void setPluginDriverUuid(java.lang.String pluginDriverUuid) {
        this.pluginDriverUuid = pluginDriverUuid;
    }
    public java.lang.String getPluginDriverUuid() {
        return this.pluginDriverUuid;
    }

    public PluginDriverInventory driver;
    public void setDriver(PluginDriverInventory driver) {
        this.driver = driver;
    }
    public PluginDriverInventory getDriver() {
        return this.driver;
    }

}
