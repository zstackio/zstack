//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class AllocationPoolType extends ApiPropertyBase {
    String start;
    String end;
    Boolean vrouter_specific_pool;
    public AllocationPoolType() {
    }
    public AllocationPoolType(String start, String end, Boolean vrouter_specific_pool) {
        this.start = start;
        this.end = end;
        this.vrouter_specific_pool = vrouter_specific_pool;
    }
    public AllocationPoolType(String start) {
        this(start, null, null);    }
    public AllocationPoolType(String start, String end) {
        this(start, end, null);    }
    
    public String getStart() {
        return start;
    }
    
    public void setStart(String start) {
        this.start = start;
    }
    
    
    public String getEnd() {
        return end;
    }
    
    public void setEnd(String end) {
        this.end = end;
    }
    
    
    public Boolean getVrouterSpecificPool() {
        return vrouter_specific_pool;
    }
    
    public void setVrouterSpecificPool(Boolean vrouter_specific_pool) {
        this.vrouter_specific_pool = vrouter_specific_pool;
    }
    
}
