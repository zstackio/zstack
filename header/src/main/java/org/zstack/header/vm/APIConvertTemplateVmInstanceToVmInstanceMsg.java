package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

@Action(category = VmInstanceConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/vm-instances/{templateVmInstanceUuid}/convert-to-vmInstance",
        method = HttpMethod.POST,
        responseClass = APIConvertTemplateVmInstanceToVmInstanceEvent.class,
        parameterName = "params"
)
public class APIConvertTemplateVmInstanceToVmInstanceMsg extends APIMessage implements VmInstanceMessage, APIAuditor {
    @APIParam(resourceType = TemplateVmInstanceVO.class, checkAccount = true)
    private String templateVmInstanceUuid;

    @APIParam(maxLength = 255, required = true)
    private String name;

    @APINoSee
    private String vmInstanceUuid;

    public String getTemplateVmInstanceUuid() {
        return templateVmInstanceUuid;
    }

    public void setTemplateVmInstanceUuid(String templateVmInstanceUuid) {
        this.templateVmInstanceUuid = templateVmInstanceUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public static APIConvertTemplateVmInstanceToVmInstanceMsg __example__() {
        APIConvertTemplateVmInstanceToVmInstanceMsg msg = new APIConvertTemplateVmInstanceToVmInstanceMsg();
        msg.setTemplateVmInstanceUuid(uuid());
        msg.setName("test-vm");
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APIConvertTemplateVmInstanceToVmInstanceEvent)rsp).getInventory().getUuid() : "", VmInstanceVO.class);
    }
}
