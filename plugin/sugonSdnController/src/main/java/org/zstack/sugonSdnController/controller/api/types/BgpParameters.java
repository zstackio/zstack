//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class BgpParameters extends ApiPropertyBase {
    Integer peer_autonomous_system;
    String peer_ip_address;
    List<String> peer_ip_address_list;
    String hold_time;
    String auth_data;
    Integer local_autonomous_system;
    Integer multihop_ttl;
    public BgpParameters() {
    }
    public BgpParameters(Integer peer_autonomous_system, String peer_ip_address, List<String> peer_ip_address_list, String hold_time, String auth_data, Integer local_autonomous_system, Integer multihop_ttl) {
        this.peer_autonomous_system = peer_autonomous_system;
        this.peer_ip_address = peer_ip_address;
        this.peer_ip_address_list = peer_ip_address_list;
        this.hold_time = hold_time;
        this.auth_data = auth_data;
        this.local_autonomous_system = local_autonomous_system;
        this.multihop_ttl = multihop_ttl;
    }
    public BgpParameters(Integer peer_autonomous_system) {
        this(peer_autonomous_system, null, null, "0", null, null, null);    }
    public BgpParameters(Integer peer_autonomous_system, String peer_ip_address) {
        this(peer_autonomous_system, peer_ip_address, null, "0", null, null, null);    }
    public BgpParameters(Integer peer_autonomous_system, String peer_ip_address, List<String> peer_ip_address_list) {
        this(peer_autonomous_system, peer_ip_address, peer_ip_address_list, "0", null, null, null);    }
    public BgpParameters(Integer peer_autonomous_system, String peer_ip_address, List<String> peer_ip_address_list, String hold_time) {
        this(peer_autonomous_system, peer_ip_address, peer_ip_address_list, hold_time, null, null, null);    }
    public BgpParameters(Integer peer_autonomous_system, String peer_ip_address, List<String> peer_ip_address_list, String hold_time, String auth_data) {
        this(peer_autonomous_system, peer_ip_address, peer_ip_address_list, hold_time, auth_data, null, null);    }
    public BgpParameters(Integer peer_autonomous_system, String peer_ip_address, List<String> peer_ip_address_list, String hold_time, String auth_data, Integer local_autonomous_system) {
        this(peer_autonomous_system, peer_ip_address, peer_ip_address_list, hold_time, auth_data, local_autonomous_system, null);    }
    
    public Integer getPeerAutonomousSystem() {
        return peer_autonomous_system;
    }
    
    public void setPeerAutonomousSystem(Integer peer_autonomous_system) {
        this.peer_autonomous_system = peer_autonomous_system;
    }
    
    
    public String getPeerIpAddress() {
        return peer_ip_address;
    }
    
    public void setPeerIpAddress(String peer_ip_address) {
        this.peer_ip_address = peer_ip_address;
    }
    
    
    public String getHoldTime() {
        return hold_time;
    }
    
    public void setHoldTime(String hold_time) {
        this.hold_time = hold_time;
    }
    
    
    public String getAuthData() {
        return auth_data;
    }
    
    public void setAuthData(String auth_data) {
        this.auth_data = auth_data;
    }
    
    
    public Integer getLocalAutonomousSystem() {
        return local_autonomous_system;
    }
    
    public void setLocalAutonomousSystem(Integer local_autonomous_system) {
        this.local_autonomous_system = local_autonomous_system;
    }
    
    
    public Integer getMultihopTtl() {
        return multihop_ttl;
    }
    
    public void setMultihopTtl(Integer multihop_ttl) {
        this.multihop_ttl = multihop_ttl;
    }
    
    
    public List<String> getPeerIpAddressList() {
        return peer_ip_address_list;
    }
    
    
    public void addPeerIpAddress(String obj) {
        if (peer_ip_address_list == null) {
            peer_ip_address_list = new ArrayList<String>();
        }
        peer_ip_address_list.add(obj);
    }
    public void clearPeerIpAddress() {
        peer_ip_address_list = null;
    }
    
}
