package org.zstack.header.vm;


import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
/**
 * Created by luchukun on 8/4/16.
 */
@Action(category = VmInstanceConstant.ACTION_CATEGORY)
public class APISetVmSshKeyMsg extends APIMessage implements VmInstanceMessage{
    @APIParam(resourceType = VmInstanceVO.class,checkAccount = true,operationTarget = true)
    private String uuid;
    @APIParam
    private String SshKey;

    @Override
    public String getVmInstanceUuid(){
        return uuid;
    }

    public String getUuid(){
        return uuid;
    }

    public void setUuid(String uuid){
        this.uuid = uuid;
    }

    public void setSshKey(String SshKey){
        this.SshKey = SshKey;
    }

    public String getSshKey(){
        return SshKey;
    }

}