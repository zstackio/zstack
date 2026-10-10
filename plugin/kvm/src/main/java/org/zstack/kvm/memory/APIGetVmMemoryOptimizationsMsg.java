package org.zstack.kvm.memory;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.message.APIParam;
import org.zstack.header.vm.VmInstanceVO;

import java.util.List;

@Action(category = org.zstack.header.vm.VmInstanceConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(path = "/vm-instances/memory-optimizations", method = HttpMethod.GET,
        responseClass = APIGetVmMemoryOptimizationsReply.class)
public class APIGetVmMemoryOptimizationsMsg extends APISyncCallMessage {
    @APIParam(resourceType = VmInstanceVO.class, nonempty = true, checkAccount = true)
    private List<String> vmUuids;
    public List<String> getVmUuids() { return vmUuids; }
    public void setVmUuids(List<String> value) { vmUuids = value; }
    public static APIGetVmMemoryOptimizationsMsg __example__() {
        APIGetVmMemoryOptimizationsMsg msg = new APIGetVmMemoryOptimizationsMsg();
        msg.setVmUuids(java.util.Collections.singletonList("1234567890abcdef1234567890abcdef"));
        return msg;
    }
}
