package org.zstack.network.service.flat;

import org.zstack.network.service.flat.FlatDhcpBackend.DeleteNamespaceCmd;

import java.io.Serializable;

/**
 * Created by xing5 on 2016/6/20.
 */
public class GCFlatDHCPDeleteNamespaceContext implements Serializable {
    private DeleteNamespaceCmd command;
    private String hostUuid;
    private String triggerHostStatus;

    public String getTriggerHostStatus() {
        return triggerHostStatus;
    }

    public void setTriggerHostStatus(String triggerHostStatus) {
        this.triggerHostStatus = triggerHostStatus;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public DeleteNamespaceCmd getCommand() {
        return command;
    }

    public void setCommand(DeleteNamespaceCmd command) {
        this.command = command;
    }
}
