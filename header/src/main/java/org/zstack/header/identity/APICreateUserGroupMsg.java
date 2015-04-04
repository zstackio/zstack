package org.zstack.header.identity;

import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

@NeedRoles(roles = {IdentityRoles.CREATE_USER_GROUP_ROLE})
public class APICreateUserGroupMsg extends APICreateMessage implements AccountMessage {
    @APIParam
    private String groupName;
    private String groupDescription;
    
    @Override
    public String getAccountUuid() {
        return this.getSession().getAccountUuid();
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGroupDescription() {
        return groupDescription;
    }

    public void setGroupDescription(String groupDescription) {
        this.groupDescription = groupDescription;
    }
}
