package org.zstack.header.vm;

import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;


/**
 * Created by luchukun on 7/29/16.
 */
@Action(category = VmInstanceConstant.ACTION_CATEGORY)
public class APISetVmConsolePasswordMsg extends APIMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class, checkAccount = true, operationTarget = true)
    private String uuid;
    @APIParam
    private String consolePassword;

    @Override
    public String getVmInstanceUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setConsolePassword(String consolePassword) {
        this.consolePassword = consolePassword;
    }

    public String getConsolePassword() {
        return consolePassword;
    }

}
