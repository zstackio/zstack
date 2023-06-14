package org.zstack.physicalNetworkInterface.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.physicalNetworkInterface.PhysicalNicActionType;

@Action(category = PysicalNetworkInterfaceConstant.ACTION_CATEGORY, adminOnly = true)
@RestRequest(
        path = "/physical-nic/{physicalNicUuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIRecoverPhysicalNicEvent.class,
        isAction = true
)
public class APIRecoverPhysicalNicMsg extends APIDeleteMessage implements APIAuditor, PhysicalNicMessage {

    @APIParam(resourceType = HostNetworkInterfaceVO.class, checkAccount = true ,operationTarget = true)
    private String physicalNicUuid;

    @APIParam
    private String actionType;

    private String hostUuid;


    public void setPhysicalNicUuid(String physicalNicUuid) {
        this.physicalNicUuid = physicalNicUuid;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    @Override
    public String getPhysicalNicUuid() {
        return physicalNicUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return null;
    }

    public static APIRecoverPhysicalNicMsg __example__() {
        APIRecoverPhysicalNicMsg msg = new APIRecoverPhysicalNicMsg();
        msg.setActionType(PhysicalNicActionType.SMARTNIC.toString());
        msg.setPhysicalNicUuid(uuid());
        return msg;
    }
}
