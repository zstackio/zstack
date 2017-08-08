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
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by frank on 8/8/2015.
 */
@Action(category = LoadBalancerConstants.ACTION_CATEGORY)
@RestRequest(
        path = "/load-balancers/listeners/{listenerUuid}/vm-instances/nics",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIAddVmNicToLoadBalancerEvent.class
)
public class APIAddVmNicToLoadBalancerMsg extends APIMessage implements LoadBalancerMessage {
    @APIParam(resourceType = VmNicVO.class, checkAccount = true, operationTarget = true, nonempty = true)
    private List<String> vmNicUuids;
    @APIParam(resourceType = LoadBalancerListenerVO.class, checkAccount = true, operationTarget = true)
    private String listenerUuid;
    @APINoSee
    private String loadBalancerUuid;

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
 
    public static APIAddVmNicToLoadBalancerMsg __example__() {
        APIAddVmNicToLoadBalancerMsg msg = new APIAddVmNicToLoadBalancerMsg();

        msg.setVmNicUuids(Arrays.asList(uuid()));
        msg.setListenerUuid(uuid());
        msg.setLoadBalancerUuid(uuid());

        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                if (evt.isSuccess()) {
                    for (String vmNicUuid : vmNicUuids) {
                        Tuple t = Q.New(VmNicVO.class)
                                .select(VmNicVO_.vmInstanceUuid, VmNicVO_.ip)
                                .eq(VmNicVO_.uuid, vmNicUuids).findTuple();


                        String vmUuid = t.get(0, String.class);
                        String ip = t.get(1, String.class);

                        ntfy("Added load balancer[uuid:%s]", loadBalancerUuid)
                                .resource(loadBalancerUuid, LoadBalancerVO.class.getSimpleName())
                                .context("vmNicUuid", vmNicUuid)
                                .context("vmUuid", vmUuid)
                                .messageAndEvent(that, evt).done();

                        ntfy("Add a load balancer[%s] to the nic[ip:%s]", loadBalancerUuid, ip)
                                .context("loadBalancerUuid", loadBalancerUuid)
                                .resource(vmUuid, VmInstanceVO.class.getSimpleName())
                                .messageAndEvent(that, evt).done();

                        ntfy("Added to the nic[ip:%s]", vmUuid, ip)
                                .resource(listenerUuid, LoadBalancerListenerVO.class.getSimpleName())
                                .context("vmUuid", vmUuid)
                                .messageAndEvent(that, evt).done();
                    }



                }
            }
        };
    }

}
