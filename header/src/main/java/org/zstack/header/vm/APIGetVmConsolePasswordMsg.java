package org.zstack.header.vm;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
/**
 * Created by root on 7/29/16.
 */
@Action(category = VmInstanceConstant.ACTION_CATEGORY, names = {"read"})
public class APIGetVmConsolePasswordMsg extends APISyncCallMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String uuid;

    public String getUuid() {
            return uuid;
        }

    public void setUuid(String uuid) {
            this.uuid = uuid;
        }

    @Override
    public String getVmInstanceUuid() {
            return getUuid();
        }
}


