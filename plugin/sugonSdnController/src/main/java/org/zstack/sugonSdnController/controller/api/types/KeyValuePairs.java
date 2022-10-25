//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class KeyValuePairs extends ApiPropertyBase {
    List<KeyValuePair> key_value_pair;
    public KeyValuePairs() {
    }
    public KeyValuePairs(List<KeyValuePair> key_value_pair) {
        this.key_value_pair = key_value_pair;
    }
    
    public List<KeyValuePair> getKeyValuePair() {
        return key_value_pair;
    }
    
    
    public void addKeyValuePair(KeyValuePair obj) {
        if (key_value_pair == null) {
            key_value_pair = new ArrayList<KeyValuePair>();
        }
        key_value_pair.add(obj);
    }
    public void clearKeyValuePair() {
        key_value_pair = null;
    }
    
    
    public void addKeyValuePair(String key, String value) {
        if (key_value_pair == null) {
            key_value_pair = new ArrayList<KeyValuePair>();
        }
        key_value_pair.add(new KeyValuePair(key, value));
    }
    
}
