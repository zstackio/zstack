package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

@Action(category = VmInstanceConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/convert-to-templateVmInstance",
        method = HttpMethod.POST,
        responseClass = APIConvertVmInstanceToTemplateVmInstanceEvent.class,
        parameterName = "params"
)
public class APIConvertVmInstanceToTemplateVmInstanceMsg extends APIMessage implements VmInstanceMessage, APIAuditor {
    @APIParam(resourceType = VmInstanceVO.class, checkAccount = true, operationTarget = true)
    private String vmInstanceUuid;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public static APIConvertVmInstanceToTemplateVmInstanceMsg __example__() {
        APIConvertVmInstanceToTemplateVmInstanceMsg msg = new APIConvertVmInstanceToTemplateVmInstanceMsg();
        msg.vmInstanceUuid = uuid();
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APIConvertVmInstanceToTemplateVmInstanceEvent)rsp).getInventory().getUuid() : "", TemplateVmInstanceVO.class);
    }
}
