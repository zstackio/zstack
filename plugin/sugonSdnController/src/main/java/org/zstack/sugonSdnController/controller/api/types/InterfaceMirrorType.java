//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class InterfaceMirrorType extends ApiPropertyBase {
    String traffic_direction;
    MirrorActionType mirror_to;
    public InterfaceMirrorType() {
    }
    public InterfaceMirrorType(String traffic_direction, MirrorActionType mirror_to) {
        this.traffic_direction = traffic_direction;
        this.mirror_to = mirror_to;
    }
    public InterfaceMirrorType(String traffic_direction) {
        this(traffic_direction, null);    }
    
    public String getTrafficDirection() {
        return traffic_direction;
    }
    
    public void setTrafficDirection(String traffic_direction) {
        this.traffic_direction = traffic_direction;
    }
    
    
    public MirrorActionType getMirrorTo() {
        return mirror_to;
    }
    
    public void setMirrorTo(MirrorActionType mirror_to) {
        this.mirror_to = mirror_to;
    }
    
}
