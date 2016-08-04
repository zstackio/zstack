package org.zstack.header.vm;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
/**
 * Created by luchukun on 8/4/16.
 */
@Action(category = VmInstanceConstant.ACTION_CATEGORY)
public class APIDeleteVmSshKeyMsg extends APIMessage implements VmInstanceMessage{
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
