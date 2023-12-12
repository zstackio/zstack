package org.zstack.header.securitymachine.api.securitymachine;

import org.springframework.http.HttpMethod;
import org.zstack.header.securitymachine.SecurityMachineMessage;
import org.zstack.header.securitymachine.SecurityMachineState;
import org.zstack.header.securitymachine.SecurityMachineVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

/**
 * Created by LiangHanYu on 2021/11/12 17:28
 */
@RestRequest(
        path = "/security-machines/{uuid}/actions",
        isAction = true,
        responseClass = APIChangeSecurityMachineStateEvent.class,
        method = HttpMethod.PUT
)
public class APIChangeSecurityMachineStateMsg extends APIMessage implements SecurityMachineMessage, APIAuditor {
    @APIParam(resourceType = SecurityMachineVO.class)
    private String uuid;

    @APIParam(validValues = {"enable", "disable"})
    private String stateEvent;

    public APIChangeSecurityMachineStateMsg() {
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getStateEvent() {
        return stateEvent;
    }

    public void setStateEvent(String stateEvent) {
        this.stateEvent = stateEvent;
    }

    public static APIChangeSecurityMachineStateMsg __example__() {
        APIChangeSecurityMachineStateMsg msg = new APIChangeSecurityMachineStateMsg();
        msg.setUuid(uuid());
        msg.setStateEvent(SecurityMachineState.Enabled.toString());
        return msg;
    }

    @Override
    public String getSecurityMachineUuid() {
        return uuid;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new APIAuditor.Result(((APIChangeSecurityMachineStateMsg) msg).getUuid(), SecurityMachineVO.class);
    }
}
