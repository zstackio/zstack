//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class FlowAgingTimeoutList extends ApiPropertyBase {
    List<FlowAgingTimeout> flow_aging_timeout;
    public FlowAgingTimeoutList() {
    }
    public FlowAgingTimeoutList(List<FlowAgingTimeout> flow_aging_timeout) {
        this.flow_aging_timeout = flow_aging_timeout;
    }
    
    public List<FlowAgingTimeout> getFlowAgingTimeout() {
        return flow_aging_timeout;
    }
    
    
    public void addFlowAgingTimeout(FlowAgingTimeout obj) {
        if (flow_aging_timeout == null) {
            flow_aging_timeout = new ArrayList<FlowAgingTimeout>();
        }
        flow_aging_timeout.add(obj);
    }
    public void clearFlowAgingTimeout() {
        flow_aging_timeout = null;
    }
    
    
    public void addFlowAgingTimeout(String protocol, Integer port, Integer timeout_in_seconds) {
        if (flow_aging_timeout == null) {
            flow_aging_timeout = new ArrayList<FlowAgingTimeout>();
        }
        flow_aging_timeout.add(new FlowAgingTimeout(protocol, port, timeout_in_seconds));
    }
    
}
