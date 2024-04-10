package org.zstack.sdk.sns.platform.plugin;



public class SNSPluginEndpointInventory extends org.zstack.sdk.sns.SNSApplicationEndpointInventory {

    public java.lang.String endpointType;
    public void setEndpointType(java.lang.String endpointType) {
        this.endpointType = endpointType;
    }
    public java.lang.String getEndpointType() {
        return this.endpointType;
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
