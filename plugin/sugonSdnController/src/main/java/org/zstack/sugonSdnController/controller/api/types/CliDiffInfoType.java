//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class CliDiffInfoType extends ApiPropertyBase {
    String username;
    String time;
    String config_changes;
    public CliDiffInfoType() {
    }
    public CliDiffInfoType(String username, String time, String config_changes) {
        this.username = username;
        this.time = time;
        this.config_changes = config_changes;
    }
    public CliDiffInfoType(String username) {
        this(username, null, null);    }
    public CliDiffInfoType(String username, String time) {
        this(username, time, null);    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    
    public String getTime() {
        return time;
    }
    
    public void setTime(String time) {
        this.time = time;
    }
    
    
    public String getConfigChanges() {
        return config_changes;
    }
    
    public void setConfigChanges(String config_changes) {
        this.config_changes = config_changes;
    }
    
}
