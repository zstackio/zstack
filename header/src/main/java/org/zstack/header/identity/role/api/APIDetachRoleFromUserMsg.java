package org.zstack.header.identity.role.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.UserVO;
import org.zstack.header.identity.role.RoleVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(path = "/identities/users/{userUuid}/roles/{roleUuid}", method = HttpMethod.DELETE, responseClass = APIDetachRoleFromUserEvent.class)
public class APIDetachRoleFromUserMsg extends APIMessage {
    @APIParam
    private String roleUuid;
    @APIParam
    private String userUuid;

    public String getRoleUuid() {
        return roleUuid;
    }

    public void setRoleUuid(String roleUuid) {
        this.roleUuid = roleUuid;
    }

    public String getUserUuid() {
        return userUuid;
    }

    public void setUserUuid(String userUuid) {
        this.userUuid = userUuid;
    }
}
