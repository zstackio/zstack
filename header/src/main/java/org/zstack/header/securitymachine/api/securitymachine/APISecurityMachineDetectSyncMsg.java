package org.zstack.header.securitymachine.api.securitymachine;

import org.springframework.http.HttpMethod;
import org.zstack.header.securitymachine.SecurityMachineMessage;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.securitymachine.SecurityMachineConstant;
import org.zstack.header.securitymachine.SecurityMachineVO;

/**
 * Created by LiangHanYu on 2022/3/30 11:31
 */
@Action(category = SecurityMachineConstant.CATEGORY)
@RestRequest(
        path = "/security-machine/{uuid}/detect/sync/actions",
        parameterName = "params",
        method = HttpMethod.POST,
        responseClass = APISecurityMachineDetectSyncEvent.class
)
public class APISecurityMachineDetectSyncMsg extends APIMessage implements SecurityMachineMessage, APIAuditor {
    @APIParam(resourceType = SecurityMachineVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APISecurityMachineDetectSyncMsg __example__() {
        APISecurityMachineDetectSyncMsg msg = new APISecurityMachineDetectSyncMsg();
        msg.setUuid(uuid());
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new APIAuditor.Result(((APISecurityMachineDetectSyncMsg) msg).getUuid(), SecurityMachineVO.class);
    }

    @Override
    public String getSecurityMachineUuid() {
        return uuid;
    }
}
