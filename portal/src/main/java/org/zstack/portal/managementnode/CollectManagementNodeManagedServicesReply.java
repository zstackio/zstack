package org.zstack.portal.managementnode;

import org.zstack.header.message.MessageReply;
import org.zstack.header.physicalserver.ManagedServiceResourceUsage;

import java.util.ArrayList;
import java.util.List;

public class CollectManagementNodeManagedServicesReply extends MessageReply {
    private List<ManagedServiceResourceUsage> services = new ArrayList<>();

    public List<ManagedServiceResourceUsage> getServices() {
        return services;
    }

    public void setServices(List<ManagedServiceResourceUsage> services) {
        this.services = services;
    }
}
