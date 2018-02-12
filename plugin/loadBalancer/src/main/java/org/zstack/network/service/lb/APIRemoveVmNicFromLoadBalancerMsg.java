package org.zstack.network.service.lb;

import org.springframework.http.HttpMethod;
import org.zstack.core.db.Q;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;

import javax.persistence.Tuple;
import java.util.Arrays;
import java.util.List;

/**
 * Created by frank on 8/8/2015.
 */
@Action(category = LoadBalancerConstants.ACTION_CATEGORY)
@RestRequest(
        path = "/load-balancers/listeners/{listenerUuid}/vm-instances/nics",
        method = HttpMethod.DELETE,
        responseClass = APIRemoveVmNicFromLoadBalancerEvent.class
)
public class APIRemoveVmNicFromLoadBalancerMsg extends APIMessage implements LoadBalancerMessage {
    @APIParam(resourceType = VmNicVO.class, checkAccount = true, operationTarget = true, nonempty = true)
    private List<String> vmNicUuids;
    @APINoSee
    private String loadBalancerUuid;
    @APIParam(resourceType = LoadBalancerListenerVO.class, checkAccount = true, operationTarget = true)
    private String listenerUuid;

    public List<String> getVmNicUuids() {
        return vmNicUuids;
    }

    public void setVmNicUuids(List<String> vmNicUuids) {
        this.vmNicUuids = vmNicUuids;
    }

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
 
    public static APIRemoveVmNicFromLoadBalancerMsg __example__() {
        APIRemoveVmNicFromLoadBalancerMsg msg = new APIRemoveVmNicFromLoadBalancerMsg();

        msg.setListenerUuid(uuid());
        msg.setVmNicUuids(Arrays.asList(uuid()));

        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                if (evt.isSuccess()) {
                    ntfy("Removed vm nics[uuid:%s]", vmNicUuids).resource(loadBalancerUuid,LoadBalancerVO.class.getSimpleName())
                            .context("nicUuids", vmNicUuids)
                            .messageAndEvent(that, evt).done();


                    for (String vmNicUuid : vmNicUuids) {
                        String vmInstanceUuid = Q.New(VmNicVO.class)
                                .select(VmNicVO_.vmInstanceUuid)
                                .eq(VmNicVO_.uuid, vmNicUuid).findValue();
                        ntfy("Removed load balancer[uuid:%s]", loadBalancerUuid).resource(vmInstanceUuid, VmInstanceVO.class.getSimpleName())
                                .context("loadBalancerUuid", loadBalancerUuid)
                                .messageAndEvent(that, evt).done();


                        ntfy("Removed from the nic[uuid:%s]", vmNicUuid)
                                .resource(listenerUuid, LoadBalancerListenerVO.class.getSimpleName())
                                .context("vmNicUuid", vmNicUuid)
                                .messageAndEvent(that, evt).done();
                    }
                }
            }
        };
    }

}
