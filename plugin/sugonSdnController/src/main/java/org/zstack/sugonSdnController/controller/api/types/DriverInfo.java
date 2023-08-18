//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class DriverInfo extends ApiPropertyBase {
    String ipmi_address;
    String deploy_ramdisk;
    String ipmi_password;
    String ipmi_port;
    String ipmi_username;
    String deploy_kernel;
    public DriverInfo() {
    }
    public DriverInfo(String ipmi_address, String deploy_ramdisk, String ipmi_password, String ipmi_port, String ipmi_username, String deploy_kernel) {
        this.ipmi_address = ipmi_address;
        this.deploy_ramdisk = deploy_ramdisk;
        this.ipmi_password = ipmi_password;
        this.ipmi_port = ipmi_port;
        this.ipmi_username = ipmi_username;
        this.deploy_kernel = deploy_kernel;
    }
    public DriverInfo(String ipmi_address) {
        this(ipmi_address, null, null, null, null, null);    }
    public DriverInfo(String ipmi_address, String deploy_ramdisk) {
        this(ipmi_address, deploy_ramdisk, null, null, null, null);    }
    public DriverInfo(String ipmi_address, String deploy_ramdisk, String ipmi_password) {
        this(ipmi_address, deploy_ramdisk, ipmi_password, null, null, null);    }
    public DriverInfo(String ipmi_address, String deploy_ramdisk, String ipmi_password, String ipmi_port) {
        this(ipmi_address, deploy_ramdisk, ipmi_password, ipmi_port, null, null);    }
    public DriverInfo(String ipmi_address, String deploy_ramdisk, String ipmi_password, String ipmi_port, String ipmi_username) {
        this(ipmi_address, deploy_ramdisk, ipmi_password, ipmi_port, ipmi_username, null);    }
    
    public String getIpmiAddress() {
        return ipmi_address;
    }
    
    public void setIpmiAddress(String ipmi_address) {
        this.ipmi_address = ipmi_address;
    }
    
    
    public String getDeployRamdisk() {
        return deploy_ramdisk;
    }
    
    public void setDeployRamdisk(String deploy_ramdisk) {
        this.deploy_ramdisk = deploy_ramdisk;
    }
    
    
    public String getIpmiPassword() {
        return ipmi_password;
    }
    
    public void setIpmiPassword(String ipmi_password) {
        this.ipmi_password = ipmi_password;
    }
    
    
    public String getIpmiPort() {
        return ipmi_port;
    }
    
    public void setIpmiPort(String ipmi_port) {
        this.ipmi_port = ipmi_port;
    }
    
    
    public String getIpmiUsername() {
        return ipmi_username;
    }
    
    public void setIpmiUsername(String ipmi_username) {
        this.ipmi_username = ipmi_username;
    }
    
    
    public String getDeployKernel() {
        return deploy_kernel;
    }
    
    public void setDeployKernel(String deploy_kernel) {
        this.deploy_kernel = deploy_kernel;
    }
    
}
