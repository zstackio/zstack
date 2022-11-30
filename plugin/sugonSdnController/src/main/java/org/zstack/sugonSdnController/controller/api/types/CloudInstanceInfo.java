//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class CloudInstanceInfo extends ApiPropertyBase {
    String os_version;
    String operating_system;
    List<String> roles;
    String availability_zone;
    String instance_type;
    String machine_id;
    Integer volume_size;
    public CloudInstanceInfo() {
    }
    public CloudInstanceInfo(String os_version, String operating_system, List<String> roles, String availability_zone, String instance_type, String machine_id, Integer volume_size) {
        this.os_version = os_version;
        this.operating_system = operating_system;
        this.roles = roles;
        this.availability_zone = availability_zone;
        this.instance_type = instance_type;
        this.machine_id = machine_id;
        this.volume_size = volume_size;
    }
    public CloudInstanceInfo(String os_version) {
        this(os_version, null, null, null, null, null, null);    }
    public CloudInstanceInfo(String os_version, String operating_system) {
        this(os_version, operating_system, null, null, null, null, null);    }
    public CloudInstanceInfo(String os_version, String operating_system, List<String> roles) {
        this(os_version, operating_system, roles, null, null, null, null);    }
    public CloudInstanceInfo(String os_version, String operating_system, List<String> roles, String availability_zone) {
        this(os_version, operating_system, roles, availability_zone, null, null, null);    }
    public CloudInstanceInfo(String os_version, String operating_system, List<String> roles, String availability_zone, String instance_type) {
        this(os_version, operating_system, roles, availability_zone, instance_type, null, null);    }
    public CloudInstanceInfo(String os_version, String operating_system, List<String> roles, String availability_zone, String instance_type, String machine_id) {
        this(os_version, operating_system, roles, availability_zone, instance_type, machine_id, null);    }
    
    public String getOsVersion() {
        return os_version;
    }
    
    public void setOsVersion(String os_version) {
        this.os_version = os_version;
    }
    
    
    public String getOperatingSystem() {
        return operating_system;
    }
    
    public void setOperatingSystem(String operating_system) {
        this.operating_system = operating_system;
    }
    
    
    public String getAvailabilityZone() {
        return availability_zone;
    }
    
    public void setAvailabilityZone(String availability_zone) {
        this.availability_zone = availability_zone;
    }
    
    
    public String getInstanceType() {
        return instance_type;
    }
    
    public void setInstanceType(String instance_type) {
        this.instance_type = instance_type;
    }
    
    
    public String getMachineId() {
        return machine_id;
    }
    
    public void setMachineId(String machine_id) {
        this.machine_id = machine_id;
    }
    
    
    public Integer getVolumeSize() {
        return volume_size;
    }
    
    public void setVolumeSize(Integer volume_size) {
        this.volume_size = volume_size;
    }
    
    
    public List<String> getRoles() {
        return roles;
    }
    
    
    public void addRoles(String obj) {
        if (roles == null) {
            roles = new ArrayList<String>();
        }
        roles.add(obj);
    }
    public void clearRoles() {
        roles = null;
    }
    
}
