package org.zstack.appliancevm;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.VmInstanceMessage;
import org.zstack.utils.gson.JSONObjectUtil;

/**
 * Created by frank on 8/22/2015.
 */
public class ApplianceVmAsyncHttpCallMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String vmInstanceUuid;
    private String path;
    private String command;
    private int commandTimeout = 300;
    private boolean checkStatus;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(Object cmd) {
        setCommandByString(JSONObjectUtil.toJsonString(cmd));
    }

    public void setCommandByString(String command) {
        this.command = command;
    }

    public int getCommandTimeout() {
        return commandTimeout;
    }

    public void setCommandTimeout(int commandTimeout) {
        this.commandTimeout = commandTimeout;
    }

    public boolean isCheckStatus() {
        return checkStatus;
    }

    public void setCheckStatus(boolean checkStatus) {
        this.checkStatus = checkStatus;
    }
}
