//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class ESXIHostInfo extends ApiPropertyBase {
    String username;
    String datacenter;
    String esxi_name;
    String cluster;
    String mac;
    String datastore;
    String password;
    String vcenter_server;
    public ESXIHostInfo() {
    }
    public ESXIHostInfo(String username, String datacenter, String esxi_name, String cluster, String mac, String datastore, String password, String vcenter_server) {
        this.username = username;
        this.datacenter = datacenter;
        this.esxi_name = esxi_name;
        this.cluster = cluster;
        this.mac = mac;
        this.datastore = datastore;
        this.password = password;
        this.vcenter_server = vcenter_server;
    }
    public ESXIHostInfo(String username) {
        this(username, null, null, null, null, null, null, null);    }
    public ESXIHostInfo(String username, String datacenter) {
        this(username, datacenter, null, null, null, null, null, null);    }
    public ESXIHostInfo(String username, String datacenter, String esxi_name) {
        this(username, datacenter, esxi_name, null, null, null, null, null);    }
    public ESXIHostInfo(String username, String datacenter, String esxi_name, String cluster) {
        this(username, datacenter, esxi_name, cluster, null, null, null, null);    }
    public ESXIHostInfo(String username, String datacenter, String esxi_name, String cluster, String mac) {
        this(username, datacenter, esxi_name, cluster, mac, null, null, null);    }
    public ESXIHostInfo(String username, String datacenter, String esxi_name, String cluster, String mac, String datastore) {
        this(username, datacenter, esxi_name, cluster, mac, datastore, null, null);    }
    public ESXIHostInfo(String username, String datacenter, String esxi_name, String cluster, String mac, String datastore, String password) {
        this(username, datacenter, esxi_name, cluster, mac, datastore, password, null);    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    
    public String getDatacenter() {
        return datacenter;
    }
    
    public void setDatacenter(String datacenter) {
        this.datacenter = datacenter;
    }
    
    
    public String getEsxiName() {
        return esxi_name;
    }
    
    public void setEsxiName(String esxi_name) {
        this.esxi_name = esxi_name;
    }
    
    
    public String getCluster() {
        return cluster;
    }
    
    public void setCluster(String cluster) {
        this.cluster = cluster;
    }
    
    
    public String getMac() {
        return mac;
    }
    
    public void setMac(String mac) {
        this.mac = mac;
    }
    
    
    public String getDatastore() {
        return datastore;
    }
    
    public void setDatastore(String datastore) {
        this.datastore = datastore;
    }
    
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    
    public String getVcenterServer() {
        return vcenter_server;
    }
    
    public void setVcenterServer(String vcenter_server) {
        this.vcenter_server = vcenter_server;
    }
    
}
