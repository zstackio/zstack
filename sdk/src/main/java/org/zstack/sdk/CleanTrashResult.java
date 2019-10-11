package org.zstack.sdk;


import java.util.List;

public class CleanTrashResult  {

    public java.util.List resourceUuids;
    public void setResourceUuids(java.util.List resourceUuids) {
        this.resourceUuids = resourceUuids;
    }
    public java.util.List getResourceUuids() {
        return this.resourceUuids;
    }

    public java.util.List details;
    public List getDetails() {
        return details;
    }
    public void setDetails(List details) {
        this.details = details;
    }

    public java.lang.Long size;
    public void setSize(java.lang.Long size) {
        this.size = size;
    }
    public java.lang.Long getSize() {
        return this.size;
    }

}
