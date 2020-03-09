package org.zstack.sdk;

import org.zstack.sdk.PublishAppStatus;

public class PublishAppResourceStruct  {

    public java.lang.String appUuid;
    public void setAppUuid(java.lang.String appUuid) {
        this.appUuid = appUuid;
    }
    public java.lang.String getAppUuid() {
        return this.appUuid;
    }

    public PublishAppStatus appStatus;
    public void setAppStatus(PublishAppStatus appStatus) {
        this.appStatus = appStatus;
    }
    public PublishAppStatus getAppStatus() {
        return this.appStatus;
    }

    public java.lang.String resourceUuid;
    public void setResourceUuid(java.lang.String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }
    public java.lang.String getResourceUuid() {
        return this.resourceUuid;
    }

    public java.lang.String resourceType;
    public void setResourceType(java.lang.String resourceType) {
        this.resourceType = resourceType;
    }
    public java.lang.String getResourceType() {
        return this.resourceType;
    }

}
