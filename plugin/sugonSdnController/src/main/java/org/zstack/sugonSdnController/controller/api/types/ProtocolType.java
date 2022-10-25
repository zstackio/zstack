//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class ProtocolType extends ApiPropertyBase {
    String protocol;
    Integer port;
    String ignore_address;
    SubnetType source_prefix;
    Integer source_aggregate_prefix_length;
    SubnetType destination_prefix;
    Integer destination_aggregate_prefix_length;
    public ProtocolType() {
    }
    public ProtocolType(String protocol, Integer port, String ignore_address, SubnetType source_prefix, Integer source_aggregate_prefix_length, SubnetType destination_prefix, Integer destination_aggregate_prefix_length) {
        this.protocol = protocol;
        this.port = port;
        this.ignore_address = ignore_address;
        this.source_prefix = source_prefix;
        this.source_aggregate_prefix_length = source_aggregate_prefix_length;
        this.destination_prefix = destination_prefix;
        this.destination_aggregate_prefix_length = destination_aggregate_prefix_length;
    }
    public ProtocolType(String protocol) {
        this(protocol, null, null, null, null, null, null);    }
    public ProtocolType(String protocol, Integer port) {
        this(protocol, port, null, null, null, null, null);    }
    public ProtocolType(String protocol, Integer port, String ignore_address) {
        this(protocol, port, ignore_address, null, null, null, null);    }
    public ProtocolType(String protocol, Integer port, String ignore_address, SubnetType source_prefix) {
        this(protocol, port, ignore_address, source_prefix, null, null, null);    }
    public ProtocolType(String protocol, Integer port, String ignore_address, SubnetType source_prefix, Integer source_aggregate_prefix_length) {
        this(protocol, port, ignore_address, source_prefix, source_aggregate_prefix_length, null, null);    }
    public ProtocolType(String protocol, Integer port, String ignore_address, SubnetType source_prefix, Integer source_aggregate_prefix_length, SubnetType destination_prefix) {
        this(protocol, port, ignore_address, source_prefix, source_aggregate_prefix_length, destination_prefix, null);    }
    
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
    
    
    public String getIgnoreAddress() {
        return ignore_address;
    }
    
    public void setIgnoreAddress(String ignore_address) {
        this.ignore_address = ignore_address;
    }
    
    
    public SubnetType getSourcePrefix() {
        return source_prefix;
    }
    
    public void setSourcePrefix(SubnetType source_prefix) {
        this.source_prefix = source_prefix;
    }
    
    
    public Integer getSourceAggregatePrefixLength() {
        return source_aggregate_prefix_length;
    }
    
    public void setSourceAggregatePrefixLength(Integer source_aggregate_prefix_length) {
        this.source_aggregate_prefix_length = source_aggregate_prefix_length;
    }
    
    
    public SubnetType getDestinationPrefix() {
        return destination_prefix;
    }
    
    public void setDestinationPrefix(SubnetType destination_prefix) {
        this.destination_prefix = destination_prefix;
    }
    
    
    public Integer getDestinationAggregatePrefixLength() {
        return destination_aggregate_prefix_length;
    }
    
    public void setDestinationAggregatePrefixLength(Integer destination_aggregate_prefix_length) {
        this.destination_aggregate_prefix_length = destination_aggregate_prefix_length;
    }
    
}
