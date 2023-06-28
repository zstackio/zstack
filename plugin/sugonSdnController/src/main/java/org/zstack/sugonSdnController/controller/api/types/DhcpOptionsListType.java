//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class DhcpOptionsListType extends ApiPropertyBase {
    List<DhcpOptionType> dhcp_option;
    public DhcpOptionsListType() {
    }
    public DhcpOptionsListType(List<DhcpOptionType> dhcp_option) {
        this.dhcp_option = dhcp_option;
    }
    
    public List<DhcpOptionType> getDhcpOption() {
        return dhcp_option;
    }
    
    
    public void addDhcpOption(DhcpOptionType obj) {
        if (dhcp_option == null) {
            dhcp_option = new ArrayList<DhcpOptionType>();
        }
        dhcp_option.add(obj);
    }
    public void clearDhcpOption() {
        dhcp_option = null;
    }
    
    
    public void addDhcpOption(String dhcp_option_name, String dhcp_option_value, String dhcp_option_value_bytes) {
        if (dhcp_option == null) {
            dhcp_option = new ArrayList<DhcpOptionType>();
        }
        dhcp_option.add(new DhcpOptionType(dhcp_option_name, dhcp_option_value, dhcp_option_value_bytes));
    }
    
}
