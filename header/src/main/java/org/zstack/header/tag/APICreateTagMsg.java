package org.zstack.header.tag;

import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

/**
 */
public abstract class APICreateTagMsg extends APIMessage {
    @APIParam
    private String resourceType;
    @APIParam(checkAccount = true)
    private String resourceUuid;
    @APIParam
    private String tag;

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

}