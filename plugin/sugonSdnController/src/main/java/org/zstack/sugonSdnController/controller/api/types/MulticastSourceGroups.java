//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class MulticastSourceGroups extends ApiPropertyBase {
    List<MulticastSourceGroup> multicast_source_group;
    public MulticastSourceGroups() {
    }
    public MulticastSourceGroups(List<MulticastSourceGroup> multicast_source_group) {
        this.multicast_source_group = multicast_source_group;
    }
    
    public List<MulticastSourceGroup> getMulticastSourceGroup() {
        return multicast_source_group;
    }
    
    
    public void addMulticastSourceGroup(MulticastSourceGroup obj) {
        if (multicast_source_group == null) {
            multicast_source_group = new ArrayList<MulticastSourceGroup>();
        }
        multicast_source_group.add(obj);
    }
    public void clearMulticastSourceGroup() {
        multicast_source_group = null;
    }
    
    
    public void addMulticastSourceGroup(String source_address, String group_address, String action) {
        if (multicast_source_group == null) {
            multicast_source_group = new ArrayList<MulticastSourceGroup>();
        }
        multicast_source_group.add(new MulticastSourceGroup(source_address, group_address, action));
    }
    
}
