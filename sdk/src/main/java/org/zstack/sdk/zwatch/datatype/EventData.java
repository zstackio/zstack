package org.zstack.sdk.zwatch.datatype;

import org.zstack.sdk.zwatch.datatype.EmergencyLevel;

public class EventData  {

    public java.lang.String LABEL_RESOURCE_ID;
    public void setLABEL_RESOURCE_ID(java.lang.String LABEL_RESOURCE_ID) {
        this.LABEL_RESOURCE_ID = LABEL_RESOURCE_ID;
    }
    public java.lang.String getLABEL_RESOURCE_ID() {
        return this.LABEL_RESOURCE_ID;
    }

    public java.lang.String LABEL_EMERGENCY_LEVEL;
    public void setLABEL_EMERGENCY_LEVEL(java.lang.String LABEL_EMERGENCY_LEVEL) {
        this.LABEL_EMERGENCY_LEVEL = LABEL_EMERGENCY_LEVEL;
    }
    public java.lang.String getLABEL_EMERGENCY_LEVEL() {
        return this.LABEL_EMERGENCY_LEVEL;
    }

    public java.lang.String LABEL_IS_API;
    public void setLABEL_IS_API(java.lang.String LABEL_IS_API) {
        this.LABEL_IS_API = LABEL_IS_API;
    }
    public java.lang.String getLABEL_IS_API() {
        return this.LABEL_IS_API;
    }

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

    public java.util.Map labels;
    public void setLabels(java.util.Map labels) {
        this.labels = labels;
    }
    public java.util.Map getLabels() {
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
