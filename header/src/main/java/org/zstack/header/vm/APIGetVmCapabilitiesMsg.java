package org.zstack.header.vm;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;

/**
 * Created by xing5 on 2016/5/17.
 */
@Action(category = VmInstanceConstant.ACTION_CATEGORY, names = {"read"})
public class APIGetVmCapabilitiesMsg extends APISyncCallMessage implements VmInstanceMessage {
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
