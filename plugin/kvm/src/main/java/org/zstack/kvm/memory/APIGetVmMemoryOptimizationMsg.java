package org.zstack.kvm.memory;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.*;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;

@Action(category = org.zstack.header.vm.VmInstanceConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(path = "/vm-instances/{vmUuid}/memory-optimization", method = HttpMethod.GET,
        responseClass = APIGetVmMemoryOptimizationReply.class)
public class APIGetVmMemoryOptimizationMsg extends APISyncCallMessage {
    @APIParam(resourceType = VmInstanceVO.class, checkAccount = true)
    private String vmUuid;
    public String getVmUuid() { return vmUuid; }
    public void setVmUuid(String value) { vmUuid = value; }
    public static APIGetVmMemoryOptimizationMsg __example__() {
        APIGetVmMemoryOptimizationMsg msg = new APIGetVmMemoryOptimizationMsg();
        msg.setVmUuid("1234567890abcdef1234567890abcdef");
        return msg;
    }
}
