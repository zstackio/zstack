//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class StormControlParameters extends ApiPropertyBase {
    List<String> storm_control_actions;
    Integer recovery_timeout;
    Boolean no_unregistered_multicast;
    Boolean no_registered_multicast;
    Boolean no_unknown_unicast;
    Boolean no_multicast;
    Boolean no_broadcast;
    Integer bandwidth_percent;
    public StormControlParameters() {
    }
    public StormControlParameters(List<String> storm_control_actions, Integer recovery_timeout, Boolean no_unregistered_multicast, Boolean no_registered_multicast, Boolean no_unknown_unicast, Boolean no_multicast, Boolean no_broadcast, Integer bandwidth_percent) {
        this.storm_control_actions = storm_control_actions;
        this.recovery_timeout = recovery_timeout;
        this.no_unregistered_multicast = no_unregistered_multicast;
        this.no_registered_multicast = no_registered_multicast;
        this.no_unknown_unicast = no_unknown_unicast;
        this.no_multicast = no_multicast;
        this.no_broadcast = no_broadcast;
        this.bandwidth_percent = bandwidth_percent;
    }
    public StormControlParameters(List<String> storm_control_actions) {
        this(storm_control_actions, null, false, false, false, false, false, null);    }
    public StormControlParameters(List<String> storm_control_actions, Integer recovery_timeout) {
        this(storm_control_actions, recovery_timeout, false, false, false, false, false, null);    }
    public StormControlParameters(List<String> storm_control_actions, Integer recovery_timeout, Boolean no_unregistered_multicast) {
        this(storm_control_actions, recovery_timeout, no_unregistered_multicast, false, false, false, false, null);    }
    public StormControlParameters(List<String> storm_control_actions, Integer recovery_timeout, Boolean no_unregistered_multicast, Boolean no_registered_multicast) {
        this(storm_control_actions, recovery_timeout, no_unregistered_multicast, no_registered_multicast, false, false, false, null);    }
    public StormControlParameters(List<String> storm_control_actions, Integer recovery_timeout, Boolean no_unregistered_multicast, Boolean no_registered_multicast, Boolean no_unknown_unicast) {
        this(storm_control_actions, recovery_timeout, no_unregistered_multicast, no_registered_multicast, no_unknown_unicast, false, false, null);    }
    public StormControlParameters(List<String> storm_control_actions, Integer recovery_timeout, Boolean no_unregistered_multicast, Boolean no_registered_multicast, Boolean no_unknown_unicast, Boolean no_multicast) {
        this(storm_control_actions, recovery_timeout, no_unregistered_multicast, no_registered_multicast, no_unknown_unicast, no_multicast, false, null);    }
    public StormControlParameters(List<String> storm_control_actions, Integer recovery_timeout, Boolean no_unregistered_multicast, Boolean no_registered_multicast, Boolean no_unknown_unicast, Boolean no_multicast, Boolean no_broadcast) {
        this(storm_control_actions, recovery_timeout, no_unregistered_multicast, no_registered_multicast, no_unknown_unicast, no_multicast, no_broadcast, null);    }
    
    public Integer getRecoveryTimeout() {
        return recovery_timeout;
    }
    
    public void setRecoveryTimeout(Integer recovery_timeout) {
        this.recovery_timeout = recovery_timeout;
    }
    
    
    public Boolean getNoUnregisteredMulticast() {
        return no_unregistered_multicast;
    }
    
    public void setNoUnregisteredMulticast(Boolean no_unregistered_multicast) {
        this.no_unregistered_multicast = no_unregistered_multicast;
    }
    
    
    public Boolean getNoRegisteredMulticast() {
        return no_registered_multicast;
    }
    
    public void setNoRegisteredMulticast(Boolean no_registered_multicast) {
        this.no_registered_multicast = no_registered_multicast;
    }
    
    
    public Boolean getNoUnknownUnicast() {
        return no_unknown_unicast;
    }
    
    public void setNoUnknownUnicast(Boolean no_unknown_unicast) {
        this.no_unknown_unicast = no_unknown_unicast;
    }
    
    
    public Boolean getNoMulticast() {
        return no_multicast;
    }
    
    public void setNoMulticast(Boolean no_multicast) {
        this.no_multicast = no_multicast;
    }
    
    
    public Boolean getNoBroadcast() {
        return no_broadcast;
    }
    
    public void setNoBroadcast(Boolean no_broadcast) {
        this.no_broadcast = no_broadcast;
    }
    
    
    public Integer getBandwidthPercent() {
        return bandwidth_percent;
    }
    
    public void setBandwidthPercent(Integer bandwidth_percent) {
        this.bandwidth_percent = bandwidth_percent;
    }
    
    
    public List<String> getStormControlActions() {
        return storm_control_actions;
    }
    
    
    public void addStormControlActions(String obj) {
        if (storm_control_actions == null) {
            storm_control_actions = new ArrayList<String>();
        }
        storm_control_actions.add(obj);
    }
    public void clearStormControlActions() {
        storm_control_actions = null;
    }
    
}
