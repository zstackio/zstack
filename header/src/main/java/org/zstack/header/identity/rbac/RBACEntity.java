package org.zstack.header.identity.rbac;

import org.zstack.header.message.APIMessage;

public class RBACEntity {
    private String apiName;
    private APIMessage apiMessage;

    public RBACEntity(APIMessage apiMessage) {
        this.apiMessage = apiMessage;
        apiName = apiMessage.getClass().getName();
    }

    public RBACEntity() {
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public APIMessage getApiMessage() {
        return apiMessage;
    }

    public void setApiMessage(APIMessage apiMessage) {
        this.apiMessage = apiMessage;
    }
}
