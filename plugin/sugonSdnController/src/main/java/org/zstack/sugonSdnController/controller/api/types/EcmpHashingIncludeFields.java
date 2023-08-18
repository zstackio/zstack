//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class EcmpHashingIncludeFields extends ApiPropertyBase {
    Boolean hashing_configured;
    Boolean source_ip;
    Boolean destination_ip;
    Boolean ip_protocol;
    Boolean source_port;
    Boolean destination_port;
    public EcmpHashingIncludeFields() {
    }
    public EcmpHashingIncludeFields(Boolean hashing_configured, Boolean source_ip, Boolean destination_ip, Boolean ip_protocol, Boolean source_port, Boolean destination_port) {
        this.hashing_configured = hashing_configured;
        this.source_ip = source_ip;
        this.destination_ip = destination_ip;
        this.ip_protocol = ip_protocol;
        this.source_port = source_port;
        this.destination_port = destination_port;
    }
    public EcmpHashingIncludeFields(Boolean hashing_configured) {
        this(hashing_configured, true, true, true, true, true);    }
    public EcmpHashingIncludeFields(Boolean hashing_configured, Boolean source_ip) {
        this(hashing_configured, source_ip, true, true, true, true);    }
    public EcmpHashingIncludeFields(Boolean hashing_configured, Boolean source_ip, Boolean destination_ip) {
        this(hashing_configured, source_ip, destination_ip, true, true, true);    }
    public EcmpHashingIncludeFields(Boolean hashing_configured, Boolean source_ip, Boolean destination_ip, Boolean ip_protocol) {
        this(hashing_configured, source_ip, destination_ip, ip_protocol, true, true);    }
    public EcmpHashingIncludeFields(Boolean hashing_configured, Boolean source_ip, Boolean destination_ip, Boolean ip_protocol, Boolean source_port) {
        this(hashing_configured, source_ip, destination_ip, ip_protocol, source_port, true);    }
    
    public Boolean getHashingConfigured() {
        return hashing_configured;
    }
    
    public void setHashingConfigured(Boolean hashing_configured) {
        this.hashing_configured = hashing_configured;
    }
    
    
    public Boolean getSourceIp() {
        return source_ip;
    }
    
    public void setSourceIp(Boolean source_ip) {
        this.source_ip = source_ip;
    }
    
    
    public Boolean getDestinationIp() {
        return destination_ip;
    }
    
    public void setDestinationIp(Boolean destination_ip) {
        this.destination_ip = destination_ip;
    }
    
    
    public Boolean getIpProtocol() {
        return ip_protocol;
    }
    
    public void setIpProtocol(Boolean ip_protocol) {
        this.ip_protocol = ip_protocol;
    }
    
    
    public Boolean getSourcePort() {
        return source_port;
    }
    
    public void setSourcePort(Boolean source_port) {
        this.source_port = source_port;
    }
    
    
    public Boolean getDestinationPort() {
        return destination_port;
    }
    
    public void setDestinationPort(Boolean destination_port) {
        this.destination_port = destination_port;
    }
    
}
