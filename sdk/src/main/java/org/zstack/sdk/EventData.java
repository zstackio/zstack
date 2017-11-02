package org.zstack.sdk;

public class EventData  {

    public java.lang.String namespace;
    public void setNamespace(java.lang.String namespace) {
        this.namespace = namespace;
    }
    public java.lang.String getNamespace() {
        return this.namespace;
    }

    public java.lang.String name;
    public void setName(java.lang.String name) {
        this.name = name;
    }
    public java.lang.String getName() {
        return this.name;
    }

    public java.util.Map<String, String> labels;
    public void setLabels(java.util.Map<String, String> labels) {
        this.labels = labels;
    }
    public java.util.Map<String, String> getLabels() {
        return this.labels;
    }

    public EmergencyLevel emergencyLevel;
    public void setEmergencyLevel(EmergencyLevel emergencyLevel) {
        this.emergencyLevel = emergencyLevel;
    }
    public EmergencyLevel getEmergencyLevel() {
        return this.emergencyLevel;
    }

    public java.lang.String resourceId;
    public void setResourceId(java.lang.String resourceId) {
        this.resourceId = resourceId;
    }
    public java.lang.String getResourceId() {
        return this.resourceId;
    }

    public java.lang.String error;
    public void setError(java.lang.String error) {
        this.error = error;
    }
    public java.lang.String getError() {
        return this.error;
    }

    public boolean api;
    public void setApi(boolean api) {
        this.api = api;
    }
    public boolean getApi() {
        return this.api;
    }

    public long time;
    public void setTime(long time) {
        this.time = time;
    }
    public long getTime() {
        return this.time;
    }

}
