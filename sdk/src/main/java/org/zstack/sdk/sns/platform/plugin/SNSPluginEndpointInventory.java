package org.zstack.sdk.sns.platform.plugin;



public class SNSPluginEndpointInventory extends org.zstack.sdk.sns.SNSApplicationEndpointInventory {

    public java.lang.String pluginDriverUuid;
    public void setPluginDriverUuid(java.lang.String pluginDriverUuid) {
        this.pluginDriverUuid = pluginDriverUuid;
    }
    public java.lang.String getPluginDriverUuid() {
        return this.pluginDriverUuid;
    }

    public long timeoutInSeconds;
    public void setTimeoutInSeconds(long timeoutInSeconds) {
        this.timeoutInSeconds = timeoutInSeconds;
    }
    public long getTimeoutInSeconds() {
        return this.timeoutInSeconds;
    }

    public java.util.Map properties;
    public void setProperties(java.util.Map properties) {
        this.properties = properties;
    }
    public java.util.Map getProperties() {
        return this.properties;
    }

}
