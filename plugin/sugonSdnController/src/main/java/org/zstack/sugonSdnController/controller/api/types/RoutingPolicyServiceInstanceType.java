//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class RoutingPolicyServiceInstanceType extends ApiPropertyBase {
    String left_sequence;
    String right_sequence;
    public RoutingPolicyServiceInstanceType() {
    }
    public RoutingPolicyServiceInstanceType(String left_sequence, String right_sequence) {
        this.left_sequence = left_sequence;
        this.right_sequence = right_sequence;
    }
    public RoutingPolicyServiceInstanceType(String left_sequence) {
        this(left_sequence, null);    }
    
    public String getLeftSequence() {
        return left_sequence;
    }
    
    public void setLeftSequence(String left_sequence) {
        this.left_sequence = left_sequence;
    }
    
    
    public String getRightSequence() {
        return right_sequence;
    }
    
    public void setRightSequence(String right_sequence) {
        this.right_sequence = right_sequence;
    }
    
}
