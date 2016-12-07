package org.zstack.network.service.lb;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 8/8/2015.
 */
@RestResponse
public class APIDeleteLoadBalancerEvent extends APIEvent {
    public APIDeleteLoadBalancerEvent() {
    }

    public APIDeleteLoadBalancerEvent(String apiId) {
        super(apiId);
    }
}
