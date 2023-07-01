//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class UserDefinedLogStatList extends ApiPropertyBase {
    List<UserDefinedLogStat> statlist;
    public UserDefinedLogStatList() {
    }
    public UserDefinedLogStatList(List<UserDefinedLogStat> statlist) {
        this.statlist = statlist;
    }
    
    public List<UserDefinedLogStat> getStatlist() {
        return statlist;
    }
    
    
    public void addStatlist(UserDefinedLogStat obj) {
        if (statlist == null) {
            statlist = new ArrayList<UserDefinedLogStat>();
        }
        statlist.add(obj);
    }
    public void clearStatlist() {
        statlist = null;
    }
    
    
    public void addStatlist(String name, String pattern) {
        if (statlist == null) {
            statlist = new ArrayList<UserDefinedLogStat>();
        }
        statlist.add(new UserDefinedLogStat(name, pattern));
    }
    
}
