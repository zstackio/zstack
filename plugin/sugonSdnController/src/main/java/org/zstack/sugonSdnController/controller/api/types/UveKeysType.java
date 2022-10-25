//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class UveKeysType extends ApiPropertyBase {
    List<String> uve_key;
    public UveKeysType() {
    }
    public UveKeysType(List<String> uve_key) {
        this.uve_key = uve_key;
    }
    
    public List<String> getUveKey() {
        return uve_key;
    }
    
    
    public void addUveKey(String obj) {
        if (uve_key == null) {
            uve_key = new ArrayList<String>();
        }
        uve_key.add(obj);
    }
    public void clearUveKey() {
        uve_key = null;
    }
    
}
