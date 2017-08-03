package org.zstack.network.service.lb;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.network.service.vip.VipVO;

/**
 * Created by camile on 5/18/2017.
 */
@Action(category = LoadBalancerConstants.ACTION_CATEGORY)
@RestRequest(
        path = "/load-balancers/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateLoadBalancerEvent.class,
        isAction = true
        //parameterName = "updateLoadBalancer"
)
public class APIUpdateLoadBalancerMsg extends APICreateMessage  implements LoadBalancerMessage {
    @APIParam(resourceType = LoadBalancerVO.class, checkAccount = true, operationTarget = true)
    private String uuid;
    @APIParam(maxLength = 255, required = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getLoadBalancerUuid(){
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public static APIUpdateLoadBalancerMsg __example__() {
        APIUpdateLoadBalancerMsg msg = new APIUpdateLoadBalancerMsg();
        msg.uuid = uuid();
        msg.setDescription("info");
        msg.setName("Test-Lb");

        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("Updated").resource(uuid, LoadBalancerVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
            }
        };
    }
}
