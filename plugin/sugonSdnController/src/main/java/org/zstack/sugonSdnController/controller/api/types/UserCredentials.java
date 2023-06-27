//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class UserCredentials extends ApiPropertyBase {
    String username;
    String password;
    public UserCredentials() {
    }
    public UserCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }
    public UserCredentials(String username) {
        this(username, null);    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
}
