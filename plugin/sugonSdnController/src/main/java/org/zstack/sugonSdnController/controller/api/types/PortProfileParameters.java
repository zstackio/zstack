//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class PortProfileParameters extends ApiPropertyBase {
    PortParameters port_params;
    Boolean flow_control;
    LacpParams lacp_params;
    Boolean bpdu_loop_protection;
    Boolean port_cos_untrust;
    public PortProfileParameters() {
    }
    public PortProfileParameters(PortParameters port_params, Boolean flow_control, LacpParams lacp_params, Boolean bpdu_loop_protection, Boolean port_cos_untrust) {
        this.port_params = port_params;
        this.flow_control = flow_control;
        this.lacp_params = lacp_params;
        this.bpdu_loop_protection = bpdu_loop_protection;
        this.port_cos_untrust = port_cos_untrust;
    }
    public PortProfileParameters(PortParameters port_params) {
        this(port_params, false, null, false, false);    }
    public PortProfileParameters(PortParameters port_params, Boolean flow_control) {
        this(port_params, flow_control, null, false, false);    }
    public PortProfileParameters(PortParameters port_params, Boolean flow_control, LacpParams lacp_params) {
        this(port_params, flow_control, lacp_params, false, false);    }
    public PortProfileParameters(PortParameters port_params, Boolean flow_control, LacpParams lacp_params, Boolean bpdu_loop_protection) {
        this(port_params, flow_control, lacp_params, bpdu_loop_protection, false);    }
    
    public PortParameters getPortParams() {
        return port_params;
    }
    
    public void setPortParams(PortParameters port_params) {
        this.port_params = port_params;
    }
    
    
    public Boolean getFlowControl() {
        return flow_control;
    }
    
    public void setFlowControl(Boolean flow_control) {
        this.flow_control = flow_control;
    }
    
    
    public LacpParams getLacpParams() {
        return lacp_params;
    }
    
    public void setLacpParams(LacpParams lacp_params) {
        this.lacp_params = lacp_params;
    }
    
    
    public Boolean getBpduLoopProtection() {
        return bpdu_loop_protection;
    }
    
    public void setBpduLoopProtection(Boolean bpdu_loop_protection) {
        this.bpdu_loop_protection = bpdu_loop_protection;
    }
    
    
    public Boolean getPortCosUntrust() {
        return port_cos_untrust;
    }
    
    public void setPortCosUntrust(Boolean port_cos_untrust) {
        this.port_cos_untrust = port_cos_untrust;
    }
    
}
