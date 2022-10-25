//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class ExecutableInfoListType extends ApiPropertyBase {
    List<ExecutableInfoType> executable_info;
    public ExecutableInfoListType() {
    }
    public ExecutableInfoListType(List<ExecutableInfoType> executable_info) {
        this.executable_info = executable_info;
    }
    
    public List<ExecutableInfoType> getExecutableInfo() {
        return executable_info;
    }
    
    
    public void addExecutableInfo(ExecutableInfoType obj) {
        if (executable_info == null) {
            executable_info = new ArrayList<ExecutableInfoType>();
        }
        executable_info.add(obj);
    }
    public void clearExecutableInfo() {
        executable_info = null;
    }
    
    
    public void addExecutableInfo(String executable_path, String executable_args, Integer job_completion_weightage) {
        if (executable_info == null) {
            executable_info = new ArrayList<ExecutableInfoType>();
        }
        executable_info.add(new ExecutableInfoType(executable_path, executable_args, job_completion_weightage));
    }
    
}
