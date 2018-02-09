package org.zstack.header.identity.role.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.UserGroupVO;
import org.zstack.header.identity.role.RoleVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(path = "/identities/user-groups/{groupUuid}/roles/{uuid}", method = HttpMethod.DELETE, responseClass = APIDetachRoleFromUserGroupEvent.class)
public class APIDetachRoleFromUserGroupMsg extends APIMessage {
    @APIParam
    private String roleUuid;
    @APIParam
    private String groupUuid;

    public String getRoleUuid() {
        return roleUuid;
    }

    public void setRoleUuid(String roleUuid) {
        this.roleUuid = roleUuid;
    }

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
    }
}
