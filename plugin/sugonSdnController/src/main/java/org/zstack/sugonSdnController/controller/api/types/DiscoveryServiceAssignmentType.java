//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class DiscoveryServiceAssignmentType extends ApiPropertyBase {
    DiscoveryPubSubEndPointType publisher;
    List<DiscoveryPubSubEndPointType> subscriber;
    public DiscoveryServiceAssignmentType() {
    }
    public DiscoveryServiceAssignmentType(DiscoveryPubSubEndPointType publisher, List<DiscoveryPubSubEndPointType> subscriber) {
        this.publisher = publisher;
        this.subscriber = subscriber;
    }
    public DiscoveryServiceAssignmentType(DiscoveryPubSubEndPointType publisher) {
        this(publisher, null);    }
    
    public DiscoveryPubSubEndPointType getPublisher() {
        return publisher;
    }
    
    public void setPublisher(DiscoveryPubSubEndPointType publisher) {
        this.publisher = publisher;
    }
    
    
    public List<DiscoveryPubSubEndPointType> getSubscriber() {
        return subscriber;
    }
    
    
    public void addSubscriber(DiscoveryPubSubEndPointType obj) {
        if (subscriber == null) {
            subscriber = new ArrayList<DiscoveryPubSubEndPointType>();
        }
        subscriber.add(obj);
    }
    public void clearSubscriber() {
        subscriber = null;
    }
    
    
    public void addSubscriber(String ep_type, String ep_id, SubnetType ep_prefix, String ep_version) {
        if (subscriber == null) {
            subscriber = new ArrayList<DiscoveryPubSubEndPointType>();
        }
        subscriber.add(new DiscoveryPubSubEndPointType(ep_type, ep_id, ep_prefix, ep_version));
    }
    
}
