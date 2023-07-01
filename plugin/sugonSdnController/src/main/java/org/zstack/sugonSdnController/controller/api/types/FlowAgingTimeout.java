//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class FlowAgingTimeout extends ApiPropertyBase {
    String protocol;
    Integer port;
    Integer timeout_in_seconds;
    public FlowAgingTimeout() {
    }
    public FlowAgingTimeout(String protocol, Integer port, Integer timeout_in_seconds) {
        this.protocol = protocol;
        this.port = port;
        this.timeout_in_seconds = timeout_in_seconds;
    }
    public FlowAgingTimeout(String protocol) {
        this(protocol, null, null);    }
    public FlowAgingTimeout(String protocol, Integer port) {
        this(protocol, port, null);    }
    
    public String getProtocol() {
        return protocol;
    }
    
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
    
    
    public Integer getPort() {
        return port;
    }
    
    public void setPort(Integer port) {
        this.port = port;
    }
    
    
    public Integer getTimeoutInSeconds() {
        return timeout_in_seconds;
    }
    
    public void setTimeoutInSeconds(Integer timeout_in_seconds) {
        this.timeout_in_seconds = timeout_in_seconds;
    }
    
}
