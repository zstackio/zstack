package org.zstack.header.vm;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
/**
 * Created by luchukun on 8/4/16.
 */
@Action(category = VmInstanceConstant.ACTION_CATEGORY, names = {"read"})
public class APIGetVmSshKeyMsg extends APISyncCallMessage implements VmInstanceMessage{
    @APIParam(resourceType = VmInstanceVO.class)
    private String uuid;

    @Override
    public String getVmInstanceUuid(){
        return uuid;
    }

    public void setUuid(String uuid){
        this.uuid = uuid;
    }

    public String getUuid(){
        return uuid;
    }
}
