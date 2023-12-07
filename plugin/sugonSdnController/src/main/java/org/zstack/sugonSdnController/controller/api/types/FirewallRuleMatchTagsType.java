//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class FirewallRuleMatchTagsType extends ApiPropertyBase {
    List<String> tag_list;
    public FirewallRuleMatchTagsType() {
    }
    public FirewallRuleMatchTagsType(List<String> tag_list) {
        this.tag_list = tag_list;
    }
    
    public List<String> getTagList() {
        return tag_list;
    }
    
    
    public void addTag(String obj) {
        if (tag_list == null) {
            tag_list = new ArrayList<String>();
        }
        tag_list.add(obj);
    }
    public void clearTag() {
        tag_list = null;
    }
    
}
