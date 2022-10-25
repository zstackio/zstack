//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class PortInfoType extends ApiPropertyBase {
    String name;
    String type_;
    String port_speed;
    Boolean channelized;
    String channelized_port_speed;
    String port_group;
    List<String> labels;
    public PortInfoType() {
    }
    public PortInfoType(String name, String type_, String port_speed, Boolean channelized, String channelized_port_speed, String port_group, List<String> labels) {
        this.name = name;
        this.type_ = type_;
        this.port_speed = port_speed;
        this.channelized = channelized;
        this.channelized_port_speed = channelized_port_speed;
        this.port_group = port_group;
        this.labels = labels;
    }
    public PortInfoType(String name) {
        this(name, null, null, false, null, null, null);    }
    public PortInfoType(String name, String type_) {
        this(name, type_, null, false, null, null, null);    }
    public PortInfoType(String name, String type_, String port_speed) {
        this(name, type_, port_speed, false, null, null, null);    }
    public PortInfoType(String name, String type_, String port_speed, Boolean channelized) {
        this(name, type_, port_speed, channelized, null, null, null);    }
    public PortInfoType(String name, String type_, String port_speed, Boolean channelized, String channelized_port_speed) {
        this(name, type_, port_speed, channelized, channelized_port_speed, null, null);    }
    public PortInfoType(String name, String type_, String port_speed, Boolean channelized, String channelized_port_speed, String port_group) {
        this(name, type_, port_speed, channelized, channelized_port_speed, port_group, null);    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    
    public String getType() {
        return type_;
    }
    
    public void setType(String type_) {
        this.type_ = type_;
    }
    
    
    public String getPortSpeed() {
        return port_speed;
    }
    
    public void setPortSpeed(String port_speed) {
        this.port_speed = port_speed;
    }
    
    
    public Boolean getChannelized() {
        return channelized;
    }
    
    public void setChannelized(Boolean channelized) {
        this.channelized = channelized;
    }
    
    
    public String getChannelizedPortSpeed() {
        return channelized_port_speed;
    }
    
    public void setChannelizedPortSpeed(String channelized_port_speed) {
        this.channelized_port_speed = channelized_port_speed;
    }
    
    
    public String getPortGroup() {
        return port_group;
    }
    
    public void setPortGroup(String port_group) {
        this.port_group = port_group;
    }
    
    
    public List<String> getLabels() {
        return labels;
    }
    
    
    public void addLabels(String obj) {
        if (labels == null) {
            labels = new ArrayList<String>();
        }
        labels.add(obj);
    }
    public void clearLabels() {
        labels = null;
    }
    
}
