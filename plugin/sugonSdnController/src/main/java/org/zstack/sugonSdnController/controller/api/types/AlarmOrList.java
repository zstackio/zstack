//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class AlarmOrList extends ApiPropertyBase {
    List<AlarmAndList> or_list;
    public AlarmOrList() {
    }
    public AlarmOrList(List<AlarmAndList> or_list) {
        this.or_list = or_list;
    }
    
    public List<AlarmAndList> getOrList() {
        return or_list;
    }
    
    
    public void addOr(AlarmAndList obj) {
        if (or_list == null) {
            or_list = new ArrayList<AlarmAndList>();
        }
        or_list.add(obj);
    }
    public void clearOr() {
        or_list = null;
    }
    
    
    public void addOr(List<AlarmExpression> and_list) {
        if (or_list == null) {
            or_list = new ArrayList<AlarmAndList>();
        }
        or_list.add(new AlarmAndList(and_list));
    }
    
}
