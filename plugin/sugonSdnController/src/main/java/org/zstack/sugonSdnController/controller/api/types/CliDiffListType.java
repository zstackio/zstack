//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class CliDiffListType extends ApiPropertyBase {
    List<CliDiffInfoType> commit_diff_info;
    public CliDiffListType() {
    }
    public CliDiffListType(List<CliDiffInfoType> commit_diff_info) {
        this.commit_diff_info = commit_diff_info;
    }
    
    public List<CliDiffInfoType> getCommitDiffInfo() {
        return commit_diff_info;
    }
    
    
    public void addCommitDiffInfo(CliDiffInfoType obj) {
        if (commit_diff_info == null) {
            commit_diff_info = new ArrayList<CliDiffInfoType>();
        }
        commit_diff_info.add(obj);
    }
    public void clearCommitDiffInfo() {
        commit_diff_info = null;
    }
    
    
    public void addCommitDiffInfo(String username, String time, String config_changes) {
        if (commit_diff_info == null) {
            commit_diff_info = new ArrayList<CliDiffInfoType>();
        }
        commit_diff_info.add(new CliDiffInfoType(username, time, config_changes));
    }
    
}
