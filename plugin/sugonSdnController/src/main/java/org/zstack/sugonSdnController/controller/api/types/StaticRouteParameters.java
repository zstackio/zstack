//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class StaticRouteParameters extends ApiPropertyBase {
    List<String> interface_route_table_uuid;
    List<String> next_hop_ip_address;
    public StaticRouteParameters() {
    }
    public StaticRouteParameters(List<String> interface_route_table_uuid, List<String> next_hop_ip_address) {
        this.interface_route_table_uuid = interface_route_table_uuid;
        this.next_hop_ip_address = next_hop_ip_address;
    }
    public StaticRouteParameters(List<String> interface_route_table_uuid) {
        this(interface_route_table_uuid, null);    }
    
    public List<String> getInterfaceRouteTableUuid() {
        return interface_route_table_uuid;
    }
    
    
    public void addInterfaceRouteTableUuid(String obj) {
        if (interface_route_table_uuid == null) {
            interface_route_table_uuid = new ArrayList<String>();
        }
        interface_route_table_uuid.add(obj);
    }
    public void clearInterfaceRouteTableUuid() {
        interface_route_table_uuid = null;
    }
    
    
    public List<String> getNextHopIpAddress() {
        return next_hop_ip_address;
    }
    
    
    public void addNextHopIpAddress(String obj) {
        if (next_hop_ip_address == null) {
            next_hop_ip_address = new ArrayList<String>();
        }
        next_hop_ip_address.add(obj);
    }
    public void clearNextHopIpAddress() {
        next_hop_ip_address = null;
    }
    
}
