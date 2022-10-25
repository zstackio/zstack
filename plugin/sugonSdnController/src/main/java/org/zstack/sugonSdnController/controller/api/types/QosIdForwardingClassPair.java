//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class QosIdForwardingClassPair extends ApiPropertyBase {
    Integer key;
    Integer forwarding_class_id;
    public QosIdForwardingClassPair() {
    }
    public QosIdForwardingClassPair(Integer key, Integer forwarding_class_id) {
        this.key = key;
        this.forwarding_class_id = forwarding_class_id;
    }
    public QosIdForwardingClassPair(Integer key) {
        this(key, null);    }
    
    public Integer getKey() {
        return key;
    }
    
    public void setKey(Integer key) {
        this.key = key;
    }
    
    
    public Integer getForwardingClassId() {
        return forwarding_class_id;
    }
    
    public void setForwardingClassId(Integer forwarding_class_id) {
        this.forwarding_class_id = forwarding_class_id;
    }
    
}
