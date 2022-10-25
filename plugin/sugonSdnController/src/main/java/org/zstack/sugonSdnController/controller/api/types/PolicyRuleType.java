//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class PolicyRuleType extends ApiPropertyBase {
    SequenceType rule_sequence;
    String rule_uuid;
    String direction;
    String protocol;
    List<AddressType> src_addresses;
    List<PortType> src_ports;
    List<String> application;
    List<AddressType> dst_addresses;
    List<PortType> dst_ports;
    ActionListType action_list;
    String ethertype;
    volatile java.util.Date created;
    volatile java.util.Date last_modified;
    public PolicyRuleType() {
    }
    public PolicyRuleType(SequenceType rule_sequence, String rule_uuid, String direction, String protocol, List<AddressType> src_addresses, List<PortType> src_ports, List<String> application, List<AddressType> dst_addresses, List<PortType> dst_ports, ActionListType action_list, String ethertype, java.util.Date created, java.util.Date last_modified) {
        this.rule_sequence = rule_sequence;
        this.rule_uuid = rule_uuid;
        this.direction = direction;
        this.protocol = protocol;
        this.src_addresses = src_addresses;
        this.src_ports = src_ports;
        this.application = application;
        this.dst_addresses = dst_addresses;
        this.dst_ports = dst_ports;
        this.action_list = action_list;
        this.ethertype = ethertype;
        this.created = created;
        this.last_modified = last_modified;
    }
    public PolicyRuleType(SequenceType rule_sequence) {
        this(rule_sequence, null, null, null, null, null, null, null, null, null, null, null, null);    }
    public PolicyRuleType(SequenceType rule_sequence, String rule_uuid) {
        this(rule_sequence, rule_uuid, null, null, null, null, null, null, null, null, null, null, null);    }
    public PolicyRuleType(SequenceType rule_sequence, String rule_uuid, String direction) {
        this(rule_sequence, rule_uuid, direction, null, null, null, null, null, null, null, null, null, null);    }
    public PolicyRuleType(SequenceType rule_sequence, String rule_uuid, String direction, String protocol) {
        this(rule_sequence, rule_uuid, direction, protocol, null, null, null, null, null, null, null, null, null);    }
    public PolicyRuleType(SequenceType rule_sequence, String rule_uuid, String direction, String protocol, List<AddressType> src_addresses) {
        this(rule_sequence, rule_uuid, direction, protocol, src_addresses, null, null, null, null, null, null, null, null);    }
    public PolicyRuleType(SequenceType rule_sequence, String rule_uuid, String direction, String protocol, List<AddressType> src_addresses, List<PortType> src_ports) {
        this(rule_sequence, rule_uuid, direction, protocol, src_addresses, src_ports, null, null, null, null, null, null, null);    }
    public PolicyRuleType(SequenceType rule_sequence, String rule_uuid, String direction, String protocol, List<AddressType> src_addresses, List<PortType> src_ports, List<String> application) {
        this(rule_sequence, rule_uuid, direction, protocol, src_addresses, src_ports, application, null, null, null, null, null, null);    }
    public PolicyRuleType(SequenceType rule_sequence, String rule_uuid, String direction, String protocol, List<AddressType> src_addresses, List<PortType> src_ports, List<String> application, List<AddressType> dst_addresses) {
        this(rule_sequence, rule_uuid, direction, protocol, src_addresses, src_ports, application, dst_addresses, null, null, null, null, null);    }
    public PolicyRuleType(SequenceType rule_sequence, String rule_uuid, String direction, String protocol, List<AddressType> src_addresses, List<PortType> src_ports, List<String> application, List<AddressType> dst_addresses, List<PortType> dst_ports) {
        this(rule_sequence, rule_uuid, direction, protocol, src_addresses, src_ports, application, dst_addresses, dst_ports, null, null, null, null);    }
    public PolicyRuleType(SequenceType rule_sequence, String rule_uuid, String direction, String protocol, List<AddressType> src_addresses, List<PortType> src_ports, List<String> application, List<AddressType> dst_addresses, List<PortType> dst_ports, ActionListType action_list) {
        this(rule_sequence, rule_uuid, direction, protocol, src_addresses, src_ports, application, dst_addresses, dst_ports, action_list, null, null, null);    }
    public PolicyRuleType(SequenceType rule_sequence, String rule_uuid, String direction, String protocol, List<AddressType> src_addresses, List<PortType> src_ports, List<String> application, List<AddressType> dst_addresses, List<PortType> dst_ports, ActionListType action_list, String ethertype) {
        this(rule_sequence, rule_uuid, direction, protocol, src_addresses, src_ports, application, dst_addresses, dst_ports, action_list, ethertype, null, null);    }
    public PolicyRuleType(SequenceType rule_sequence, String rule_uuid, String direction, String protocol, List<AddressType> src_addresses, List<PortType> src_ports, List<String> application, List<AddressType> dst_addresses, List<PortType> dst_ports, ActionListType action_list, String ethertype, java.util.Date created) {
        this(rule_sequence, rule_uuid, direction, protocol, src_addresses, src_ports, application, dst_addresses, dst_ports, action_list, ethertype, created, null);    }
    
    public SequenceType getRuleSequence() {
        return rule_sequence;
    }
    
    public void setRuleSequence(SequenceType rule_sequence) {
        this.rule_sequence = rule_sequence;
    }
    
    
    public String getRuleUuid() {
        return rule_uuid;
    }
    
    public void setRuleUuid(String rule_uuid) {
        this.rule_uuid = rule_uuid;
    }
    
    
    public String getDirection() {
        return direction;
    }
    
    public void setDirection(String direction) {
        this.direction = direction;
    }
    
    
    public String getProtocol() {
        return protocol;
    }
    
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
    
    
    public ActionListType getActionList() {
        return action_list;
    }
    
    public void setActionList(ActionListType action_list) {
        this.action_list = action_list;
    }
    
    
    public String getEthertype() {
        return ethertype;
    }
    
    public void setEthertype(String ethertype) {
        this.ethertype = ethertype;
    }
    
    
    public java.util.Date getCreated() {
        return created;
    }
    
    public void setCreated(java.util.Date created) {
        this.created = created;
    }
    
    
    public java.util.Date getLastModified() {
        return last_modified;
    }
    
    public void setLastModified(java.util.Date last_modified) {
        this.last_modified = last_modified;
    }
    
    
    public List<AddressType> getSrcAddresses() {
        return src_addresses;
    }
    
    
    public void addSrcAddresses(AddressType obj) {
        if (src_addresses == null) {
            src_addresses = new ArrayList<AddressType>();
        }
        src_addresses.add(obj);
    }
    public void clearSrcAddresses() {
        src_addresses = null;
    }
    
    
    public List<PortType> getSrcPorts() {
        return src_ports;
    }
    
    
    public void addSrcPorts(PortType obj) {
        if (src_ports == null) {
            src_ports = new ArrayList<PortType>();
        }
        src_ports.add(obj);
    }
    public void clearSrcPorts() {
        src_ports = null;
    }
    
    
    public void addSrcPorts(Integer start_port, Integer end_port) {
        if (src_ports == null) {
            src_ports = new ArrayList<PortType>();
        }
        src_ports.add(new PortType(start_port, end_port));
    }
    
    
    public List<String> getApplication() {
        return application;
    }
    
    
    public void addApplication(String obj) {
        if (application == null) {
            application = new ArrayList<String>();
        }
        application.add(obj);
    }
    public void clearApplication() {
        application = null;
    }
    
    
    public List<AddressType> getDstAddresses() {
        return dst_addresses;
    }
    
    
    public void addDstAddresses(AddressType obj) {
        if (dst_addresses == null) {
            dst_addresses = new ArrayList<AddressType>();
        }
        dst_addresses.add(obj);
    }
    public void clearDstAddresses() {
        dst_addresses = null;
    }
    
    
    public List<PortType> getDstPorts() {
        return dst_ports;
    }
    
    
    public void addDstPorts(PortType obj) {
        if (dst_ports == null) {
            dst_ports = new ArrayList<PortType>();
        }
        dst_ports.add(obj);
    }
    public void clearDstPorts() {
        dst_ports = null;
    }
    
    
    public void addDstPorts(Integer start_port, Integer end_port) {
        if (dst_ports == null) {
            dst_ports = new ArrayList<PortType>();
        }
        dst_ports.add(new PortType(start_port, end_port));
    }
    
}
