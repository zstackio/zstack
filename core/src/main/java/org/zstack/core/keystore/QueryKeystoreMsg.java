package org.zstack.core.keystore;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by miao on 16-8-15.
 */
public class QueryKeystoreMsg extends NeedReplyMessage {
    private String uuid;
    private String resourceUuid;
    private String resourceType;
    private String type;

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
