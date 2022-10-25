//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class TelemetryStateInfo extends ApiPropertyBase {
    List<TelemetryResourceInfo> resource;
    String server_ip;
    Integer server_port;
    public TelemetryStateInfo() {
    }
    public TelemetryStateInfo(List<TelemetryResourceInfo> resource, String server_ip, Integer server_port) {
        this.resource = resource;
        this.server_ip = server_ip;
        this.server_port = server_port;
    }
    public TelemetryStateInfo(List<TelemetryResourceInfo> resource) {
        this(resource, null, null);    }
    public TelemetryStateInfo(List<TelemetryResourceInfo> resource, String server_ip) {
        this(resource, server_ip, null);    }
    
    public String getServerIp() {
        return server_ip;
    }
    
    public void setServerIp(String server_ip) {
        this.server_ip = server_ip;
    }
    
    
    public Integer getServerPort() {
        return server_port;
    }
    
    public void setServerPort(Integer server_port) {
        this.server_port = server_port;
    }
    
    
    public List<TelemetryResourceInfo> getResource() {
        return resource;
    }
    
    
    public void addResource(TelemetryResourceInfo obj) {
        if (resource == null) {
            resource = new ArrayList<TelemetryResourceInfo>();
        }
        resource.add(obj);
    }
    public void clearResource() {
        resource = null;
    }
    
    
    public void addResource(String name, String path, String rate) {
        if (resource == null) {
            resource = new ArrayList<TelemetryResourceInfo>();
        }
        resource.add(new TelemetryResourceInfo(name, path, rate));
    }
    
}
