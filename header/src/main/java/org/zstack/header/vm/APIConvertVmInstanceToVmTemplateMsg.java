package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/convert-to-vmTemplate",
        method = HttpMethod.POST,
        responseClass = APIConvertVmInstanceToVmTemplateEvent.class,
        parameterName = "params"
)
public class APIConvertVmInstanceToVmTemplateMsg extends APICreateMessage {
    @APIParam(resourceType = VmInstanceVO.class, checkAccount = true, operationTarget = true)
    private String vmInstanceUuid;
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
