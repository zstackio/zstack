package org.zstack.header.identity;

import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

@NeedRoles(roles = {IdentityRoles.ATTACH_USER_TO_USER_GROUP_ROLE})
public class APIAttachUserToUserGroupMsg extends APIMessage implements AccountMessage {
    @APIParam
    private String userUuid;
    @APIParam
    private String groupUuid;
    
    @Override
    public String getAccountUuid() {
        return this.getSession().getAccountUuid();
    }

    public String getUserUuid() {
        return userUuid;
    }

    public void setUserUuid(String userUuid) {
        this.userUuid = userUuid;
    }

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
    }
}
