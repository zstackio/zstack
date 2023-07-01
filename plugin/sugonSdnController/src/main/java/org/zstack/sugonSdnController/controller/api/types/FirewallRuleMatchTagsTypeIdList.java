//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class FirewallRuleMatchTagsTypeIdList extends ApiPropertyBase {
    List<Integer> tag_type;
    public FirewallRuleMatchTagsTypeIdList() {
    }
    public FirewallRuleMatchTagsTypeIdList(List<Integer> tag_type) {
        this.tag_type = tag_type;
    }
    
    public List<Integer> getTagType() {
        return tag_type;
    }
    
    
    public void addTagType(Integer obj) {
        if (tag_type == null) {
            tag_type = new ArrayList<Integer>();
        }
        tag_type.add(obj);
    }
    public void clearTagType() {
        tag_type = null;
    }
    
}
