//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class EncapsulationPrioritiesType extends ApiPropertyBase {
    List<String> encapsulation;
    public EncapsulationPrioritiesType() {
    }
    public EncapsulationPrioritiesType(List<String> encapsulation) {
        this.encapsulation = encapsulation;
    }
    
    public List<String> getEncapsulation() {
        return encapsulation;
    }
    
    
    public void addEncapsulation(String obj) {
        if (encapsulation == null) {
            encapsulation = new ArrayList<String>();
        }
        encapsulation.add(obj);
    }
    public void clearEncapsulation() {
        encapsulation = null;
    }
    
}
