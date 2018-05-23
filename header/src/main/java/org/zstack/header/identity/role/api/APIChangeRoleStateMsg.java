package org.zstack.header.identity.role.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.role.RoleStateEvent;
import org.zstack.header.identity.role.RoleVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(path = "/identities/roles/{uuid}/actions", method = HttpMethod.PUT,
        responseClass = APIChangeRoleStateEvent.class, isAction = true)
public class APIChangeRoleStateMsg extends APIMessage implements RoleMessage {
    @APIParam(resourceType = RoleVO.class)
    private String uuid;
    @APIParam(validValues = {"enable", "disable"})
    private RoleStateEvent stateEvent;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public RoleStateEvent getStateEvent() {
        return stateEvent;
    }

    public void setStateEvent(RoleStateEvent stateEvent) {
        this.stateEvent = stateEvent;
    }

    @Override
    public String getRoleUuid() {
        return uuid;
    }

    public static APIChangeRoleStateMsg __example__() {
        APIChangeRoleStateMsg msg = new APIChangeRoleStateMsg();

        msg.setUuid(uuid());
        msg.setStateEvent(RoleStateEvent.enable);

        return msg;
    }
}
