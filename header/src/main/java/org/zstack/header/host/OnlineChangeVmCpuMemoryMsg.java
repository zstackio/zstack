package org.zstack.header.host;

import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.message.NeedReplyMessage;
/**
 * Created by luchukun on 8/9/16.
 */
public class OnlineChangeVmCpuMemoryMsg extends NeedReplyMessage implements HostMessage {
    private String vmInstanceUuid;
    private String hostUuid;
    private InstanceOfferingInventory instanceOfferingInventory;

    public void setVmInstanceUuid(String vmInstanceUuid){
        this.vmInstanceUuid = vmInstanceUuid;
    }
    public String getVmInstanceUuid(){
        return vmInstanceUuid;
    }

    public void setInstanceOfferingInventory(InstanceOfferingInventory instanceOfferingInventory){
        this.instanceOfferingInventory = instanceOfferingInventory;
    }
    public InstanceOfferingInventory getInstanceOfferingInventory(){
        return instanceOfferingInventory;
    }

    public void setHostUuid(String hostUuid){
        this.hostUuid = hostUuid;
    }
    @Override
    public String getHostUuid(){
        return hostUuid;
    }
}
