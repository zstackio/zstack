package org.zstack.header.host;

import org.zstack.header.configuration.InstanceOffering;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmInstanceInventory;

/**
 * Created by luchukun on 8/9/16.
 */
public class OnlineChangeVmCpuMemoryReply extends MessageReply {
    private InstanceOfferingInventory instanceOfferingInventory;

    public InstanceOfferingInventory getInstanceOfferingInventory() {
        return instanceOfferingInventory;
    }
    public void setInstanceOfferingInventory(InstanceOfferingInventory instanceOfferingInventory) {
        this.instanceOfferingInventory = instanceOfferingInventory;
    }
}

