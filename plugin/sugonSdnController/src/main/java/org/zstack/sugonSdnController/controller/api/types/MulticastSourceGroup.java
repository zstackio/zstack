//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class MulticastSourceGroup extends ApiPropertyBase {
    String source_address;
    String group_address;
    String action;
    public MulticastSourceGroup() {
    }
    public MulticastSourceGroup(String source_address, String group_address, String action) {
        this.source_address = source_address;
        this.group_address = group_address;
        this.action = action;
    }
    public MulticastSourceGroup(String source_address) {
        this(source_address, null, null);    }
    public MulticastSourceGroup(String source_address, String group_address) {
        this(source_address, group_address, null);    }
    
    public String getSourceAddress() {
        return source_address;
    }
    
    public void setSourceAddress(String source_address) {
        this.source_address = source_address;
    }
    
    
    public String getGroupAddress() {
        return group_address;
    }
    
    public void setGroupAddress(String group_address) {
        this.group_address = group_address;
    }
    
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
}
