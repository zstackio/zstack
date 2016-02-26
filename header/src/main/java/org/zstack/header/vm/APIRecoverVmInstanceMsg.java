package org.zstack.header.vm;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

/**
 * Created by frank on 11/12/2015.
 */
@Action(category = VmInstanceConstant.ACTION_CATEGORY)
public class APIRecoverVmInstanceMsg extends APIMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class, checkAccount = true, operationTarget = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return uuid;
    }
}
