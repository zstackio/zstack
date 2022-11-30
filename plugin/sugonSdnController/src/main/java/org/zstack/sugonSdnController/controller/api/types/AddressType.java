//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class AddressType extends ApiPropertyBase {
    SubnetType subnet;
    String virtual_network;
    String security_group;
    String network_policy;
    List<SubnetType> subnet_list;
    public AddressType() {
    }
    public AddressType(SubnetType subnet, String virtual_network, String security_group, String network_policy, List<SubnetType> subnet_list) {
        this.subnet = subnet;
        this.virtual_network = virtual_network;
        this.security_group = security_group;
        this.network_policy = network_policy;
        this.subnet_list = subnet_list;
    }
    public AddressType(SubnetType subnet) {
        this(subnet, null, null, null, null);    }
    public AddressType(SubnetType subnet, String virtual_network) {
        this(subnet, virtual_network, null, null, null);    }
    public AddressType(SubnetType subnet, String virtual_network, String security_group) {
        this(subnet, virtual_network, security_group, null, null);    }
    public AddressType(SubnetType subnet, String virtual_network, String security_group, String network_policy) {
        this(subnet, virtual_network, security_group, network_policy, null);    }
    
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
    
    
    public String getSecurityGroup() {
        return security_group;
    }
    
    public void setSecurityGroup(String security_group) {
        this.security_group = security_group;
    }
    
    
    public String getNetworkPolicy() {
        return network_policy;
    }
    
    public void setNetworkPolicy(String network_policy) {
        this.network_policy = network_policy;
    }
    
    
    public List<SubnetType> getSubnetList() {
        return subnet_list;
    }
    
    
    public void addSubnet(SubnetType obj) {
        if (subnet_list == null) {
            subnet_list = new ArrayList<SubnetType>();
        }
        subnet_list.add(obj);
    }
    public void clearSubnet() {
        subnet_list = null;
    }
    
    
    public void addSubnet(String ip_prefix, Integer ip_prefix_len) {
        if (subnet_list == null) {
            subnet_list = new ArrayList<SubnetType>();
        }
        subnet_list.add(new SubnetType(ip_prefix, ip_prefix_len));
    }
    
}
