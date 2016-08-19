package org.zstack.core.keystore;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;

/**
 * Created by miao on 16-8-15.
 */
@Action(category = KeystoreConstant.ACTION_CATEGORY)
public class APICreateKeystoreMsg extends APISyncCallMessage {
    @APIParam
    private String resourceUuid;

    @APIParam
    private String resourceType;

    @APIParam(validValues = {"Certificate", "SSH-KEY", "Password"})
    private String type;

    @APIParam
    private String content;


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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
