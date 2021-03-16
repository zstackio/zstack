package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

@Action(category = VmInstanceConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/vm-instances/nics/{vmNicUuid}/l3-networks-candidates",
        method = HttpMethod.GET,
        responseClass = APIGetCandidateL3NetworksForChangeVmNicNetworkReply.class
)
public class APIGetCandidateL3NetworksForChangeVmNicNetworkMsg  extends APISyncCallMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmNicVO.class, checkAccount = true)
    private String vmNicUuid;

    @APINoSee
    private String vmInstanceUuid;

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }

    public static APIGetCandidateL3NetworksForChangeVmNicNetworkMsg __example__() {
        APIGetCandidateL3NetworksForChangeVmNicNetworkMsg msg = new APIGetCandidateL3NetworksForChangeVmNicNetworkMsg();
        msg.vmNicUuid = uuid();
        return msg;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
