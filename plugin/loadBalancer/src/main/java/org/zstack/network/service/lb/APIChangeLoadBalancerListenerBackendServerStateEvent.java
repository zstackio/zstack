package org.zstack.network.service.lb;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "results")
public class APIChangeLoadBalancerListenerBackendServerStateEvent extends APIEvent {
    private List<LoadBalancerBackendServerStateResultInventory> results;

    public APIChangeLoadBalancerListenerBackendServerStateEvent() {
    }

    public APIChangeLoadBalancerListenerBackendServerStateEvent(String apiId) {
        super(apiId);
    }

    public List<LoadBalancerBackendServerStateResultInventory> getResults() {
        return results;
    }

    public void setResults(List<LoadBalancerBackendServerStateResultInventory> results) {
        this.results = results;
    }
}
