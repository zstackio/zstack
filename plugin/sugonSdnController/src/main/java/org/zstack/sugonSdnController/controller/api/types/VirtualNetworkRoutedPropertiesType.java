//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class VirtualNetworkRoutedPropertiesType extends ApiPropertyBase {
    List<RoutedProperties> routed_properties;
    Boolean shared_across_all_lrs;
    public VirtualNetworkRoutedPropertiesType() {
    }
    public VirtualNetworkRoutedPropertiesType(List<RoutedProperties> routed_properties, Boolean shared_across_all_lrs) {
        this.routed_properties = routed_properties;
        this.shared_across_all_lrs = shared_across_all_lrs;
    }
    public VirtualNetworkRoutedPropertiesType(List<RoutedProperties> routed_properties) {
        this(routed_properties, false);    }
    
    public Boolean getSharedAcrossAllLrs() {
        return shared_across_all_lrs;
    }
    
    public void setSharedAcrossAllLrs(Boolean shared_across_all_lrs) {
        this.shared_across_all_lrs = shared_across_all_lrs;
    }
    
    
    public List<RoutedProperties> getRoutedProperties() {
        return routed_properties;
    }
    
    
    public void addRoutedProperties(RoutedProperties obj) {
        if (routed_properties == null) {
            routed_properties = new ArrayList<RoutedProperties>();
        }
        routed_properties.add(obj);
    }
    public void clearRoutedProperties() {
        routed_properties = null;
    }
    
}
