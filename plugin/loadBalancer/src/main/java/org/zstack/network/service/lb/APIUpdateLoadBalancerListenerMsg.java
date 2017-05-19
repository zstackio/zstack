package org.zstack.network.service.lb;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;

/**
 * Created by camile on 5/19/2017.
 */
@Action(category = LoadBalancerConstants.ACTION_CATEGORY)
@RestRequest(
        path = "/load-balancers/listeners/{uuid}",
        method = HttpMethod.PUT,
        responseClass = APIUpdateLoadBalancerListenerEvent.class,
        isAction = true
)
public class APIUpdateLoadBalancerListenerMsg extends APIMessage implements LoadBalancerListenerMsg , LoadBalancerMessage {
    @APIParam(resourceType = LoadBalancerListenerVO.class, checkAccount = true, operationTarget = true)
    private String uuid;
    @APIParam(maxLength = 255, required = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APINoSee
    private String loadBalancerUuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }

    @Override
    public String getLoadBalancerListenerUuid() {
        return uuid;
    }

    public static APIUpdateLoadBalancerListenerMsg __example__() {
        APIUpdateLoadBalancerListenerMsg msg = new APIUpdateLoadBalancerListenerMsg();

        msg.setUuid(uuid());
        msg.setName("Test-Listener");
        msg.setDescription("desc info");

        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                if (evt.isSuccess()) {
                    ntfy("Update loadBalancer listener").resource(((APIUpdateLoadBalancerListenerEvent)evt).getInventory().getUuid(),LoadBalancerListenerVO.class.getSimpleName())
                            .messageAndEvent(that, evt).done();
                }
            }
        };
    }
}
