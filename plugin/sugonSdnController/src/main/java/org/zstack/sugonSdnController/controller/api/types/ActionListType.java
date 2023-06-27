//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class ActionListType extends ApiPropertyBase {
    String simple_action;
    String gateway_name;
    List<String> apply_service;
    ServicePropertiesType service_properties;
    MirrorActionType mirror_to;
    String assign_routing_instance;
    Boolean log;
    Boolean alert;
    String qos_action;
    Boolean host_based_service;
    public ActionListType() {
    }
    public ActionListType(String simple_action, String gateway_name, List<String> apply_service, ServicePropertiesType service_properties, MirrorActionType mirror_to, String assign_routing_instance, Boolean log, Boolean alert, String qos_action, Boolean host_based_service) {
        this.simple_action = simple_action;
        this.gateway_name = gateway_name;
        this.apply_service = apply_service;
        this.service_properties = service_properties;
        this.mirror_to = mirror_to;
        this.assign_routing_instance = assign_routing_instance;
        this.log = log;
        this.alert = alert;
        this.qos_action = qos_action;
        this.host_based_service = host_based_service;
    }
    public ActionListType(String simple_action) {
        this(simple_action, null, null, null, null, null, false, false, null, false);    }
    public ActionListType(String simple_action, String gateway_name) {
        this(simple_action, gateway_name, null, null, null, null, false, false, null, false);    }
    public ActionListType(String simple_action, String gateway_name, List<String> apply_service) {
        this(simple_action, gateway_name, apply_service, null, null, null, false, false, null, false);    }
    public ActionListType(String simple_action, String gateway_name, List<String> apply_service, ServicePropertiesType service_properties) {
        this(simple_action, gateway_name, apply_service, service_properties, null, null, false, false, null, false);    }
    public ActionListType(String simple_action, String gateway_name, List<String> apply_service, ServicePropertiesType service_properties, MirrorActionType mirror_to) {
        this(simple_action, gateway_name, apply_service, service_properties, mirror_to, null, false, false, null, false);    }
    public ActionListType(String simple_action, String gateway_name, List<String> apply_service, ServicePropertiesType service_properties, MirrorActionType mirror_to, String assign_routing_instance) {
        this(simple_action, gateway_name, apply_service, service_properties, mirror_to, assign_routing_instance, false, false, null, false);    }
    public ActionListType(String simple_action, String gateway_name, List<String> apply_service, ServicePropertiesType service_properties, MirrorActionType mirror_to, String assign_routing_instance, Boolean log) {
        this(simple_action, gateway_name, apply_service, service_properties, mirror_to, assign_routing_instance, log, false, null, false);    }
    public ActionListType(String simple_action, String gateway_name, List<String> apply_service, ServicePropertiesType service_properties, MirrorActionType mirror_to, String assign_routing_instance, Boolean log, Boolean alert) {
        this(simple_action, gateway_name, apply_service, service_properties, mirror_to, assign_routing_instance, log, alert, null, false);    }
    public ActionListType(String simple_action, String gateway_name, List<String> apply_service, ServicePropertiesType service_properties, MirrorActionType mirror_to, String assign_routing_instance, Boolean log, Boolean alert, String qos_action) {
        this(simple_action, gateway_name, apply_service, service_properties, mirror_to, assign_routing_instance, log, alert, qos_action, false);    }
    
    public String getSimpleAction() {
        return simple_action;
    }
    
    public void setSimpleAction(String simple_action) {
        this.simple_action = simple_action;
    }
    
    
    public String getGatewayName() {
        return gateway_name;
    }
    
    public void setGatewayName(String gateway_name) {
        this.gateway_name = gateway_name;
    }
    
    
    public ServicePropertiesType getServiceProperties() {
        return service_properties;
    }
    
    public void setServiceProperties(ServicePropertiesType service_properties) {
        this.service_properties = service_properties;
    }
    
    
    public MirrorActionType getMirrorTo() {
        return mirror_to;
    }
    
    public void setMirrorTo(MirrorActionType mirror_to) {
        this.mirror_to = mirror_to;
    }
    
    
    public String getAssignRoutingInstance() {
        return assign_routing_instance;
    }
    
    public void setAssignRoutingInstance(String assign_routing_instance) {
        this.assign_routing_instance = assign_routing_instance;
    }
    
    
    public Boolean getLog() {
        return log;
    }
    
    public void setLog(Boolean log) {
        this.log = log;
    }
    
    
    public Boolean getAlert() {
        return alert;
    }
    
    public void setAlert(Boolean alert) {
        this.alert = alert;
    }
    
    
    public String getQosAction() {
        return qos_action;
    }
    
    public void setQosAction(String qos_action) {
        this.qos_action = qos_action;
    }
    
    
    public Boolean getHostBasedService() {
        return host_based_service;
    }
    
    public void setHostBasedService(Boolean host_based_service) {
        this.host_based_service = host_based_service;
    }
    
    
    public List<String> getApplyService() {
        return apply_service;
    }
    
    
    public void addApplyService(String obj) {
        if (apply_service == null) {
            apply_service = new ArrayList<String>();
        }
        apply_service.add(obj);
    }
    public void clearApplyService() {
        apply_service = null;
    }
    
}
