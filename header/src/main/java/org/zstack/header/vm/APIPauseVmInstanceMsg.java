package org.zstack.header.vm;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

/**
 * Created by root on 10/29/16.
 */
@Action(category = VmInstanceConstant.ACTION_CATEGORY)
public class APIPauseVmInstanceMsg extends APIMessage implements VmInstanceMessage{
    @APIParam(resourceType = VmInstanceVO.class,checkAccount = true,operationTarget = true)
    private String uuid;

    @Override
    public String getVmInstanceUuid(){
        return getUuid();
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

}
