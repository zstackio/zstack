package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostVO;
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

    @APIParam(maxLength = 255, required = true)
    private String name;

    @APIParam(required = false, validValues = { "InstantStart", "JustConvert" })
    private String strategy = VmTemplateConversionStrategy.InstantStart.toString();

    @APIParam(required = false, resourceType = HostVO.class)
    private String hostUuid;

    public String getVmTemplateUuid() {
        return vmTemplateUuid;
    }

    public void setVmTemplateUuid(String vmTemplateUuid) {
        this.vmTemplateUuid = vmTemplateUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public static APIConvertVmTemplateToVmInstanceMsg __example__() {
        APIConvertVmTemplateToVmInstanceMsg msg = new APIConvertVmTemplateToVmInstanceMsg();
        msg.setVmTemplateUuid(uuid());
        msg.setName("test-vm");
        msg.setHostUuid(uuid());
        msg.setStrategy(VmTemplateConversionStrategy.InstantStart.toString());
        return msg;
    }
}
