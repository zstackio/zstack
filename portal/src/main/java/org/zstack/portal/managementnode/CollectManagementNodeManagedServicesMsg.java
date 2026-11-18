package org.zstack.portal.managementnode;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.physicalserver.ResourceConsumerHandle;

import java.util.ArrayList;
import java.util.List;

public class CollectManagementNodeManagedServicesMsg extends NeedReplyMessage {
    private String serverUuid;
    private String sliceName;
    private List<ResourceConsumerHandle> handles = new ArrayList<>();

    public String getServerUuid() {
        return serverUuid;
    }

    public void setServerUuid(String serverUuid) {
        this.serverUuid = serverUuid;
    }

    public String getSliceName() {
        return sliceName;
    }

    public void setSliceName(String sliceName) {
        this.sliceName = sliceName;
    }

    public List<ResourceConsumerHandle> getHandles() {
        return handles;
    }

    public void setHandles(List<ResourceConsumerHandle> handles) {
        this.handles = handles;
    }
}
