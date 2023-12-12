package org.zstack.header.securitymachine.api.securitymachine;

import org.springframework.http.HttpMethod;
import org.zstack.header.securitymachine.SecurityMachineMessage;
import org.zstack.header.securitymachine.SecurityMachineVO;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

/**
 * Created by LiangHanYu on 2021/11/3 18:22
 */

@RestRequest(
        path = "/security-machines/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteSecurityMachineEvent.class
)
public class APIDeleteSecurityMachineMsg extends APIDeleteMessage implements SecurityMachineMessage, APIAuditor {
    @APIParam(resourceType = SecurityMachineVO.class, successIfResourceNotExisting = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIDeleteSecurityMachineMsg __example__() {
        APIDeleteSecurityMachineMsg msg = new APIDeleteSecurityMachineMsg();
        msg.setUuid(uuid());
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new APIAuditor.Result(((APIDeleteSecurityMachineMsg) msg).getUuid(), SecurityMachineVO.class);
    }

    @Override
    public String getSecurityMachineUuid() {
        return uuid;
    }
}
