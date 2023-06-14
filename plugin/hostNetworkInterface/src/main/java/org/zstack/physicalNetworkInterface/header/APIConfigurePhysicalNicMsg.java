package org.zstack.physicalNetworkInterface.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APICreateMessage;
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
        responseClass = APIConfigurePhysicalNicEvent.class,
        isAction = true
)
public class APIConfigurePhysicalNicMsg extends APICreateMessage implements APIAuditor {

    @APIParam(resourceType = HostNetworkInterfaceVO.class)
    private String physicalNicUuid;

    @APIParam
    private String actionType;

    @APIParam
    private Integer virtPartNum;

    private String hostUuid;

    public String getPhysicalNicUuid() {
        return physicalNicUuid;
    }

    public void setPhysicalNicUuid(String physicalNicUuid) {
        this.physicalNicUuid = physicalNicUuid;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public Integer getVirtPartNum() {
        return virtPartNum;
    }

    public void setVirtPartNum(Integer virtPartNum) {
        this.virtPartNum = virtPartNum;
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

    public static APIConfigurePhysicalNicMsg __example__() {
        APIConfigurePhysicalNicMsg msg = new APIConfigurePhysicalNicMsg();
        msg.setActionType(PhysicalNicActionType.SMARTNIC.toString());
        msg.setPhysicalNicUuid(uuid());
        msg.setVirtPartNum(4);
        return msg;
    }
}
