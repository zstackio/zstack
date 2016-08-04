package org.zstack.header.vm;

import org.zstack.header.message.APIReply;


/**
 * Created by luchukun on 8/4/16.
 */
public class APIGetVmSshKeyReply extends APIReply{
    private String sshKey;

    public void setSshKey(String sshKey){
        this.sshKey = sshKey;
    }

    public String getSshKey(){
        return sshKey;
    }
}
