package org.zstack.network.service.lb;

import org.zstack.header.message.APIEvent;

/**
 * Created by frank on 8/8/2015.
 */
public class APIDeleteLoadBalancerEvent extends APIEvent {
    public APIDeleteLoadBalancerEvent() {
    }

    public APIDeleteLoadBalancerEvent(String apiId) {
        super(apiId);
    }
}
