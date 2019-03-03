package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 7/18/2015.
 */
@Action(category = VmInstanceConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/vm-instances/nics/{vmNicUuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDetachL3NetworkFromVmEvent.class
)
public class APIDetachL3NetworkFromVmMsg extends APIMessage implements VmInstanceMessage, APIAuditor {
    @APIParam(resourceType = VmNicVO.class, checkAccount = true, operationTarget = true)
    private String vmNicUuid;
    @APINoSee
    private String vmInstanceUuid;

    // for audit purpose only
    @APINoSee
    public String l3Uuid;

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
 
    public static APIDetachL3NetworkFromVmMsg __example__() {
        APIDetachL3NetworkFromVmMsg msg = new APIDetachL3NetworkFromVmMsg();
        msg.vmNicUuid = uuid();
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(((APIDetachL3NetworkFromVmMsg)msg).l3Uuid, L3NetworkVO.class);
    }
}
