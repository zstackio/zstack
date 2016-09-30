package org.zstack.network.service.lb;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

/**
 * Created by frank on 8/13/2015.
 */
public class LoadBalancerDeactiveVmNicMsg extends NeedReplyMessage implements LoadBalancerMessage {
    private String loadBalancerUuid;
    private List<String> vmNicUuids;
    private String listenerUuid;

    public String getListenerUuid() {
        return listenerUuid;
    }

    public void setListenerUuid(String listenerUuid) {
        this.listenerUuid = listenerUuid;
    }

    @Override
    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }

    public List<String> getVmNicUuids() {
        return vmNicUuids;
    }

    public void setVmNicUuids(List<String> vmNicUuids) {
        this.vmNicUuids = vmNicUuids;
    }
}
