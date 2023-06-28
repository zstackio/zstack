//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class CommunityAttributes extends ApiPropertyBase {
    List<String> community_attribute;
    public CommunityAttributes() {
    }
    public CommunityAttributes(List<String> community_attribute) {
        this.community_attribute = community_attribute;
    }
    
    public List<String> getCommunityAttribute() {
        return community_attribute;
    }
    
    
    public void addCommunityAttribute(String obj) {
        if (community_attribute == null) {
            community_attribute = new ArrayList<String>();
        }
        community_attribute.add(obj);
    }
    public void clearCommunityAttribute() {
        community_attribute = null;
    }
    
}
