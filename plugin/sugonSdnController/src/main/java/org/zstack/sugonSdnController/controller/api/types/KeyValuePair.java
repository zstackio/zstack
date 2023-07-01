//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class KeyValuePair extends ApiPropertyBase {
    String key;
    String value;
    public KeyValuePair() {
    }
    public KeyValuePair(String key, String value) {
        this.key = key;
        this.value = value;
    }
    public KeyValuePair(String key) {
        this(key, null);    }
    
    public String getKey() {
        return key;
    }
    
    public void setKey(String key) {
        this.key = key;
    }
    
    
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value = value;
    }
    
}
