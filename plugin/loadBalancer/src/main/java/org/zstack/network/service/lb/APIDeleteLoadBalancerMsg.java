package org.zstack.network.service.lb;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

/**
 * Created by frank on 8/8/2015.
 */
@Action(category = LoadBalancerConstants.ACTION_CATEGORY)
@RestRequest(
        path = "/load-balancers/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteLoadBalancerEvent.class
)
public class APIDeleteLoadBalancerMsg extends APIDeleteMessage implements LoadBalancerMessage {
    @APIParam(resourceType = LoadBalancerVO.class, successIfResourceNotExisting = true, checkAccount = true, operationTarget = true)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getLoadBalancerUuid() {
        return uuid;
    }
 
    public static APIDeleteLoadBalancerMsg __example__() {
        APIDeleteLoadBalancerMsg msg = new APIDeleteLoadBalancerMsg();
        msg.setUuid(uuid());
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("Deleted").resource(uuid, LoadBalancerVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
            }
        };
    }
}
