package org.zstack.abstraction.sns;

import java.util.Map;

/**
 * PluginEndpointData used to copy sns message data for plugin.
 */
public class PluginEndpointData {
    private Map metadata;
    private String message;
    private Map properties;

    public Map getMetadata() {
        return metadata;
    }

    public void setMetadata(Map metadata) {
        this.metadata = metadata;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map getProperties() {
        return properties;
    }

    public void setProperties(Map properties) {
        this.properties = properties;
    }
}
