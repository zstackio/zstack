package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/vm-instances/{vmTemplateUuid}/convert-to-vmInstance",
        method = HttpMethod.POST,
        responseClass = APIConvertVmTemplateToVmInstanceEvent.class,
        parameterName = "params"
)
public class APIConvertVmTemplateToVmInstanceMsg extends APICreateMessage {
    @APIParam(resourceType = VmTemplateVO.class, checkAccount = true)
    private String vmTemplateUuid;

    public String getVmTemplateUuid() {
        return vmTemplateUuid;
    }

    public void setVmTemplateUuid(String vmTemplateUuid) {
        this.vmTemplateUuid = vmTemplateUuid;
    }

    public static APIConvertVmTemplateToVmInstanceMsg __example__() {
        APIConvertVmTemplateToVmInstanceMsg msg = new APIConvertVmTemplateToVmInstanceMsg();
        msg.setVmTemplateUuid(uuid());
        return msg;
    }
}
