//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class QosIdForwardingClassPairs extends ApiPropertyBase {
    List<QosIdForwardingClassPair> qos_id_forwarding_class_pair;
    public QosIdForwardingClassPairs() {
    }
    public QosIdForwardingClassPairs(List<QosIdForwardingClassPair> qos_id_forwarding_class_pair) {
        this.qos_id_forwarding_class_pair = qos_id_forwarding_class_pair;
    }
    
    public List<QosIdForwardingClassPair> getQosIdForwardingClassPair() {
        return qos_id_forwarding_class_pair;
    }
    
    
    public void addQosIdForwardingClassPair(QosIdForwardingClassPair obj) {
        if (qos_id_forwarding_class_pair == null) {
            qos_id_forwarding_class_pair = new ArrayList<QosIdForwardingClassPair>();
        }
        qos_id_forwarding_class_pair.add(obj);
    }
    public void clearQosIdForwardingClassPair() {
        qos_id_forwarding_class_pair = null;
    }
    
    
    public void addQosIdForwardingClassPair(Integer key, Integer forwarding_class_id) {
        if (qos_id_forwarding_class_pair == null) {
            qos_id_forwarding_class_pair = new ArrayList<QosIdForwardingClassPair>();
        }
        qos_id_forwarding_class_pair.add(new QosIdForwardingClassPair(key, forwarding_class_id));
    }
    
}
