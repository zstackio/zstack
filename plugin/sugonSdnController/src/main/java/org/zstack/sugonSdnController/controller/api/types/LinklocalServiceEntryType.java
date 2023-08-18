//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class LinklocalServiceEntryType extends ApiPropertyBase {
    String linklocal_service_name;
    String linklocal_service_ip;
    Integer linklocal_service_port;
    String ip_fabric_DNS_service_name;
    Integer ip_fabric_service_port;
    List<String> ip_fabric_service_ip;
    public LinklocalServiceEntryType() {
    }
    public LinklocalServiceEntryType(String linklocal_service_name, String linklocal_service_ip, Integer linklocal_service_port, String ip_fabric_DNS_service_name, Integer ip_fabric_service_port, List<String> ip_fabric_service_ip) {
        this.linklocal_service_name = linklocal_service_name;
        this.linklocal_service_ip = linklocal_service_ip;
        this.linklocal_service_port = linklocal_service_port;
        this.ip_fabric_DNS_service_name = ip_fabric_DNS_service_name;
        this.ip_fabric_service_port = ip_fabric_service_port;
        this.ip_fabric_service_ip = ip_fabric_service_ip;
    }
    public LinklocalServiceEntryType(String linklocal_service_name) {
        this(linklocal_service_name, null, null, null, null, null);    }
    public LinklocalServiceEntryType(String linklocal_service_name, String linklocal_service_ip) {
        this(linklocal_service_name, linklocal_service_ip, null, null, null, null);    }
    public LinklocalServiceEntryType(String linklocal_service_name, String linklocal_service_ip, Integer linklocal_service_port) {
        this(linklocal_service_name, linklocal_service_ip, linklocal_service_port, null, null, null);    }
    public LinklocalServiceEntryType(String linklocal_service_name, String linklocal_service_ip, Integer linklocal_service_port, String ip_fabric_DNS_service_name) {
        this(linklocal_service_name, linklocal_service_ip, linklocal_service_port, ip_fabric_DNS_service_name, null, null);    }
    public LinklocalServiceEntryType(String linklocal_service_name, String linklocal_service_ip, Integer linklocal_service_port, String ip_fabric_DNS_service_name, Integer ip_fabric_service_port) {
        this(linklocal_service_name, linklocal_service_ip, linklocal_service_port, ip_fabric_DNS_service_name, ip_fabric_service_port, null);    }
    
    public String getLinklocalServiceName() {
        return linklocal_service_name;
    }
    
    public void setLinklocalServiceName(String linklocal_service_name) {
        this.linklocal_service_name = linklocal_service_name;
    }
    
    
    public String getLinklocalServiceIp() {
        return linklocal_service_ip;
    }
    
    public void setLinklocalServiceIp(String linklocal_service_ip) {
        this.linklocal_service_ip = linklocal_service_ip;
    }
    
    
    public Integer getLinklocalServicePort() {
        return linklocal_service_port;
    }
    
    public void setLinklocalServicePort(Integer linklocal_service_port) {
        this.linklocal_service_port = linklocal_service_port;
    }
    
    
    public String getIpFabricDnsServiceName() {
        return ip_fabric_DNS_service_name;
    }
    
    public void setIpFabricDnsServiceName(String ip_fabric_DNS_service_name) {
        this.ip_fabric_DNS_service_name = ip_fabric_DNS_service_name;
    }
    
    
    public Integer getIpFabricServicePort() {
        return ip_fabric_service_port;
    }
    
    public void setIpFabricServicePort(Integer ip_fabric_service_port) {
        this.ip_fabric_service_port = ip_fabric_service_port;
    }
    
    
    public List<String> getIpFabricServiceIp() {
        return ip_fabric_service_ip;
    }
    
    
    public void addIpFabricServiceIp(String obj) {
        if (ip_fabric_service_ip == null) {
            ip_fabric_service_ip = new ArrayList<String>();
        }
        ip_fabric_service_ip.add(obj);
    }
    public void clearIpFabricServiceIp() {
        ip_fabric_service_ip = null;
    }
    
}
