package org.zstack.network.service.virtualrouter;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceMessage;

@Action(category = VirtualRouterConstant.ACTION_CATEGORY, names = {"read"})
@RestRequest(
        path = "/vm-instances/appliances/virtual-routers/{vmInstanceUuid}/attachable-public-l3s",
        method = HttpMethod.GET,
        responseClass = APIGetAttachablePublicL3ForVRouterReply.class
)
public class APIGetAttachablePublicL3ForVRouterMsg extends APISyncCallMessage implements VmInstanceMessage {
    @APIParam(resourceType = VirtualRouterVmVO.class, checkAccount = true)
    private String vmInstanceUuid;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public static APIGetAttachablePublicL3ForVRouterMsg __example__() {
        APIGetAttachablePublicL3ForVRouterMsg msg = new APIGetAttachablePublicL3ForVRouterMsg();

        msg.setVmInstanceUuid(uuid());

        return msg;
    }
}
