//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class RoutingPolicyParameters extends ApiPropertyBase {
    List<String> import_routing_policy_uuid;
    List<String> export_routing_policy_uuid;
    public RoutingPolicyParameters() {
    }
    public RoutingPolicyParameters(List<String> import_routing_policy_uuid, List<String> export_routing_policy_uuid) {
        this.import_routing_policy_uuid = import_routing_policy_uuid;
        this.export_routing_policy_uuid = export_routing_policy_uuid;
    }
    public RoutingPolicyParameters(List<String> import_routing_policy_uuid) {
        this(import_routing_policy_uuid, null);    }
    
    public List<String> getImportRoutingPolicyUuid() {
        return import_routing_policy_uuid;
    }
    
    
    public void addImportRoutingPolicyUuid(String obj) {
        if (import_routing_policy_uuid == null) {
            import_routing_policy_uuid = new ArrayList<String>();
        }
        import_routing_policy_uuid.add(obj);
    }
    public void clearImportRoutingPolicyUuid() {
        import_routing_policy_uuid = null;
    }
    
    
    public List<String> getExportRoutingPolicyUuid() {
        return export_routing_policy_uuid;
    }
    
    
    public void addExportRoutingPolicyUuid(String obj) {
        if (export_routing_policy_uuid == null) {
            export_routing_policy_uuid = new ArrayList<String>();
        }
        export_routing_policy_uuid.add(obj);
    }
    public void clearExportRoutingPolicyUuid() {
        export_routing_policy_uuid = null;
    }
    
}
