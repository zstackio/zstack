package org.zstack.network.service.lb;

import org.zstack.core.Platform;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.Arrays;

/**
 * Created by shixin on 03/22/2018.
 */
@RestResponse(allTo = "inventory")
public class APIChangeLoadBalancerListenerCertificateEvent extends APIEvent {
    private LoadBalancerListenerInventory inventory;

    public APIChangeLoadBalancerListenerCertificateEvent() {
    }

    public APIChangeLoadBalancerListenerCertificateEvent(String apiId) {
        super(apiId);
    }

    public LoadBalancerListenerInventory getInventory() {
        return inventory;
    }

    public void setInventory(LoadBalancerListenerInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIChangeLoadBalancerListenerCertificateEvent __example__() {
        APIChangeLoadBalancerListenerCertificateEvent event = new APIChangeLoadBalancerListenerCertificateEvent();
        LoadBalancerListenerInventory listener = new LoadBalancerListenerInventory();
        LoadBalancerListenerVmNicRefInventory lvnr = new LoadBalancerListenerVmNicRefInventory();
        LoadBalancerListenerCertificateRefInventory ref = new LoadBalancerListenerCertificateRefInventory();

        String listenerUuid = uuid();

        lvnr.setListenerUuid(listenerUuid);
        lvnr.setVmNicUuid(uuid());

        listener.setLoadBalancerUuid(listenerUuid);
        listener.setName("Test-Lb-Listener");
        listener.setVmNicRefs(Arrays.asList(lvnr));

        ref.setCertificateUuid(uuid());
        ref.setListenerUuid(listenerUuid);
        listener.setCertificateRefs(Arrays.asList(ref));

        event.setInventory(listener);

        return event;
    }

}
