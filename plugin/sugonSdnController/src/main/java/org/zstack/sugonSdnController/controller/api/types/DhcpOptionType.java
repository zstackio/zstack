//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class DhcpOptionType extends ApiPropertyBase {
    String dhcp_option_name;
    String dhcp_option_value;
    String dhcp_option_value_bytes;
    public DhcpOptionType() {
    }
    public DhcpOptionType(String dhcp_option_name, String dhcp_option_value, String dhcp_option_value_bytes) {
        this.dhcp_option_name = dhcp_option_name;
        this.dhcp_option_value = dhcp_option_value;
        this.dhcp_option_value_bytes = dhcp_option_value_bytes;
    }
    public DhcpOptionType(String dhcp_option_name) {
        this(dhcp_option_name, null, null);    }
    public DhcpOptionType(String dhcp_option_name, String dhcp_option_value) {
        this(dhcp_option_name, dhcp_option_value, null);    }
    
    public String getDhcpOptionName() {
        return dhcp_option_name;
    }
    
    public void setDhcpOptionName(String dhcp_option_name) {
        this.dhcp_option_name = dhcp_option_name;
    }
    
    
    public String getDhcpOptionValue() {
        return dhcp_option_value;
    }
    
    public void setDhcpOptionValue(String dhcp_option_value) {
        this.dhcp_option_value = dhcp_option_value;
    }
    
    
    public String getDhcpOptionValueBytes() {
        return dhcp_option_value_bytes;
    }
    
    public void setDhcpOptionValueBytes(String dhcp_option_value_bytes) {
        this.dhcp_option_value_bytes = dhcp_option_value_bytes;
    }
    
}
