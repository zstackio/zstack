//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class ProviderDetails extends ApiPropertyBase {
    Integer segmentation_id;
    String physical_network;
    public ProviderDetails() {
    }
    public ProviderDetails(Integer segmentation_id, String physical_network) {
        this.segmentation_id = segmentation_id;
        this.physical_network = physical_network;
    }
    public ProviderDetails(Integer segmentation_id) {
        this(segmentation_id, null);    }
    
    public Integer getSegmentationId() {
        return segmentation_id;
    }
    
    public void setSegmentationId(Integer segmentation_id) {
        this.segmentation_id = segmentation_id;
    }
    
    
    public String getPhysicalNetwork() {
        return physical_network;
    }
    
    public void setPhysicalNetwork(String physical_network) {
        this.physical_network = physical_network;
    }
    
}
