package org.zstack.portal.managementnode;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.physicalserver.ResourceConsumerHandle;

import java.util.ArrayList;
import java.util.List;

public class RestartManagementNodeManagedServicesMsg extends NeedReplyMessage {
    private String serverUuid;
    private List<ResourceConsumerHandle> consumers = new ArrayList<>();

    public String getServerUuid() {
        return serverUuid;
    }

    public void setServerUuid(String serverUuid) {
        this.serverUuid = serverUuid;
    }

    public List<ResourceConsumerHandle> getConsumers() {
        return consumers;
    }

    public void setConsumers(List<ResourceConsumerHandle> consumers) {
        this.consumers = consumers;
    }
}
