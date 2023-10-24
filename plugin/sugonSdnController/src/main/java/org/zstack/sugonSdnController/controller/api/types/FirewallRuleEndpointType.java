//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class FirewallRuleEndpointType extends ApiPropertyBase {
    SubnetType subnet;
    String virtual_network;
    String address_group;
    List<String> tags;
    List<Integer> tag_ids;
    Boolean any;
    public FirewallRuleEndpointType() {
    }
    public FirewallRuleEndpointType(SubnetType subnet, String virtual_network, String address_group, List<String> tags, List<Integer> tag_ids, Boolean any) {
        this.subnet = subnet;
        this.virtual_network = virtual_network;
        this.address_group = address_group;
        this.tags = tags;
        this.tag_ids = tag_ids;
        this.any = any;
    }
    public FirewallRuleEndpointType(SubnetType subnet) {
        this(subnet, null, null, null, null, null);    }
    public FirewallRuleEndpointType(SubnetType subnet, String virtual_network) {
        this(subnet, virtual_network, null, null, null, null);    }
    public FirewallRuleEndpointType(SubnetType subnet, String virtual_network, String address_group) {
        this(subnet, virtual_network, address_group, null, null, null);    }
    public FirewallRuleEndpointType(SubnetType subnet, String virtual_network, String address_group, List<String> tags) {
        this(subnet, virtual_network, address_group, tags, null, null);    }
    public FirewallRuleEndpointType(SubnetType subnet, String virtual_network, String address_group, List<String> tags, List<Integer> tag_ids) {
        this(subnet, virtual_network, address_group, tags, tag_ids, null);    }
    
    public SubnetType getSubnet() {
        return subnet;
    }
    
    public void setSubnet(SubnetType subnet) {
        this.subnet = subnet;
    }
    
    
    public String getVirtualNetwork() {
        return virtual_network;
    }
    
    public void setVirtualNetwork(String virtual_network) {
        this.virtual_network = virtual_network;
    }
    
    
    public String getAddressGroup() {
        return address_group;
    }
    
    public void setAddressGroup(String address_group) {
        this.address_group = address_group;
    }
    
    
    public Boolean getAny() {
        return any;
    }
    
    public void setAny(Boolean any) {
        this.any = any;
    }
    
    
    public List<String> getTags() {
        return tags;
    }
    
    
    public void addTags(String obj) {
        if (tags == null) {
            tags = new ArrayList<String>();
        }
        tags.add(obj);
    }
    public void clearTags() {
        tags = null;
    }
    
    
    public List<Integer> getTagIds() {
        return tag_ids;
    }
    
    
    public void addTagIds(Integer obj) {
        if (tag_ids == null) {
            tag_ids = new ArrayList<Integer>();
        }
        tag_ids.add(obj);
    }
    public void clearTagIds() {
        tag_ids = null;
    }
    
}
