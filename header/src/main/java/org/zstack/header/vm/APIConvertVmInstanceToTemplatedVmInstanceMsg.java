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
        path = "/vm-instances/{vmInstanceUuid}/convert-to-templatedVmInstance",
        method = HttpMethod.POST,
        responseClass = APIConvertVmInstanceToTemplatedVmInstanceEvent.class,
        parameterName = "params"
)
public class APIConvertVmInstanceToTemplatedVmInstanceMsg extends APIMessage implements VmInstanceMessage, APIAuditor {
    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public static APIConvertVmInstanceToTemplatedVmInstanceMsg __example__() {
        APIConvertVmInstanceToTemplatedVmInstanceMsg msg = new APIConvertVmInstanceToTemplatedVmInstanceMsg();
        msg.vmInstanceUuid = uuid();
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APIConvertVmInstanceToTemplatedVmInstanceEvent)rsp).getInventory().getUuid() : "", TemplatedVmInstanceVO.class);
    }
}
