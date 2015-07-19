package org.zstack.header.vm;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;

/**
 * Created by frank on 7/18/2015.
 */
@Action(category = VmInstanceConstant.ACTION_CATEGORY)
public class APIDetachNicFromVmMsg extends APIMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmNicVO.class, checkAccount = true, operationTarget = true)
    private String nicUuid;
    @APINoSee
    private String vmInstanceUuid;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getNicUuid() {
        return nicUuid;
    }

    public void setNicUuid(String nicUuid) {
        this.nicUuid = nicUuid;
    }
}
