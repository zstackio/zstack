package org.zstack.network.service.virtualrouter;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.vm.VmInstanceMessage;

/**
 */
@Action(category = VirtualRouterConstant.ACTION_CATEGORY)
public class APIReconnectVirtualRouterMsg extends APIMessage implements VmInstanceMessage {
    @APIParam(resourceType = VirtualRouterVmVO.class, checkAccount = true, operationTarget = true)
    private String vmInstanceUuid;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }
}
