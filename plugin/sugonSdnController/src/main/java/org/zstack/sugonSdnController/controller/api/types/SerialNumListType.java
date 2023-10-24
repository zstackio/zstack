//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class SerialNumListType extends ApiPropertyBase {
    List<String> serial_num;
    public SerialNumListType() {
    }
    public SerialNumListType(List<String> serial_num) {
        this.serial_num = serial_num;
    }
    
    public List<String> getSerialNum() {
        return serial_num;
    }
    
    
    public void addSerialNum(String obj) {
        if (serial_num == null) {
            serial_num = new ArrayList<String>();
        }
        serial_num.add(obj);
    }
    public void clearSerialNum() {
        serial_num = null;
    }
    
}
