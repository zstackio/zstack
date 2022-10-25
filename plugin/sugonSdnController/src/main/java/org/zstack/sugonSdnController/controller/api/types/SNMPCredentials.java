//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class SNMPCredentials extends ApiPropertyBase {
    Integer version;
    Integer local_port;
    Integer retries;
    Integer timeout;
    String v2_community;
    String v3_security_name;
    String v3_security_level;
    String v3_security_engine_id;
    String v3_context;
    String v3_context_engine_id;
    String v3_authentication_protocol;
    String v3_authentication_password;
    String v3_privacy_protocol;
    String v3_privacy_password;
    String v3_engine_id;
    Integer v3_engine_boots;
    Integer v3_engine_time;
    public SNMPCredentials() {
    }
    public SNMPCredentials(Integer version, Integer local_port, Integer retries, Integer timeout, String v2_community, String v3_security_name, String v3_security_level, String v3_security_engine_id, String v3_context, String v3_context_engine_id, String v3_authentication_protocol, String v3_authentication_password, String v3_privacy_protocol, String v3_privacy_password, String v3_engine_id, Integer v3_engine_boots, Integer v3_engine_time) {
        this.version = version;
        this.local_port = local_port;
        this.retries = retries;
        this.timeout = timeout;
        this.v2_community = v2_community;
        this.v3_security_name = v3_security_name;
        this.v3_security_level = v3_security_level;
        this.v3_security_engine_id = v3_security_engine_id;
        this.v3_context = v3_context;
        this.v3_context_engine_id = v3_context_engine_id;
        this.v3_authentication_protocol = v3_authentication_protocol;
        this.v3_authentication_password = v3_authentication_password;
        this.v3_privacy_protocol = v3_privacy_protocol;
        this.v3_privacy_password = v3_privacy_password;
        this.v3_engine_id = v3_engine_id;
        this.v3_engine_boots = v3_engine_boots;
        this.v3_engine_time = v3_engine_time;
    }
    public SNMPCredentials(Integer version) {
        this(version, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);    }
    public SNMPCredentials(Integer version, Integer local_port) {
        this(version, local_port, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);    }
    public SNMPCredentials(Integer version, Integer local_port, Integer retries) {
        this(version, local_port, retries, null, null, null, null, null, null, null, null, null, null, null, null, null, null);    }
    public SNMPCredentials(Integer version, Integer local_port, Integer retries, Integer timeout) {
        this(version, local_port, retries, timeout, null, null, null, null, null, null, null, null, null, null, null, null, null);    }
    public SNMPCredentials(Integer version, Integer local_port, Integer retries, Integer timeout, String v2_community) {
        this(version, local_port, retries, timeout, v2_community, null, null, null, null, null, null, null, null, null, null, null, null);    }
    public SNMPCredentials(Integer version, Integer local_port, Integer retries, Integer timeout, String v2_community, String v3_security_name) {
        this(version, local_port, retries, timeout, v2_community, v3_security_name, null, null, null, null, null, null, null, null, null, null, null);    }
    public SNMPCredentials(Integer version, Integer local_port, Integer retries, Integer timeout, String v2_community, String v3_security_name, String v3_security_level) {
        this(version, local_port, retries, timeout, v2_community, v3_security_name, v3_security_level, null, null, null, null, null, null, null, null, null, null);    }
    public SNMPCredentials(Integer version, Integer local_port, Integer retries, Integer timeout, String v2_community, String v3_security_name, String v3_security_level, String v3_security_engine_id) {
        this(version, local_port, retries, timeout, v2_community, v3_security_name, v3_security_level, v3_security_engine_id, null, null, null, null, null, null, null, null, null);    }
    public SNMPCredentials(Integer version, Integer local_port, Integer retries, Integer timeout, String v2_community, String v3_security_name, String v3_security_level, String v3_security_engine_id, String v3_context) {
        this(version, local_port, retries, timeout, v2_community, v3_security_name, v3_security_level, v3_security_engine_id, v3_context, null, null, null, null, null, null, null, null);    }
    public SNMPCredentials(Integer version, Integer local_port, Integer retries, Integer timeout, String v2_community, String v3_security_name, String v3_security_level, String v3_security_engine_id, String v3_context, String v3_context_engine_id) {
        this(version, local_port, retries, timeout, v2_community, v3_security_name, v3_security_level, v3_security_engine_id, v3_context, v3_context_engine_id, null, null, null, null, null, null, null);    }
    public SNMPCredentials(Integer version, Integer local_port, Integer retries, Integer timeout, String v2_community, String v3_security_name, String v3_security_level, String v3_security_engine_id, String v3_context, String v3_context_engine_id, String v3_authentication_protocol) {
        this(version, local_port, retries, timeout, v2_community, v3_security_name, v3_security_level, v3_security_engine_id, v3_context, v3_context_engine_id, v3_authentication_protocol, null, null, null, null, null, null);    }
    public SNMPCredentials(Integer version, Integer local_port, Integer retries, Integer timeout, String v2_community, String v3_security_name, String v3_security_level, String v3_security_engine_id, String v3_context, String v3_context_engine_id, String v3_authentication_protocol, String v3_authentication_password) {
        this(version, local_port, retries, timeout, v2_community, v3_security_name, v3_security_level, v3_security_engine_id, v3_context, v3_context_engine_id, v3_authentication_protocol, v3_authentication_password, null, null, null, null, null);    }
    public SNMPCredentials(Integer version, Integer local_port, Integer retries, Integer timeout, String v2_community, String v3_security_name, String v3_security_level, String v3_security_engine_id, String v3_context, String v3_context_engine_id, String v3_authentication_protocol, String v3_authentication_password, String v3_privacy_protocol) {
        this(version, local_port, retries, timeout, v2_community, v3_security_name, v3_security_level, v3_security_engine_id, v3_context, v3_context_engine_id, v3_authentication_protocol, v3_authentication_password, v3_privacy_protocol, null, null, null, null);    }
    public SNMPCredentials(Integer version, Integer local_port, Integer retries, Integer timeout, String v2_community, String v3_security_name, String v3_security_level, String v3_security_engine_id, String v3_context, String v3_context_engine_id, String v3_authentication_protocol, String v3_authentication_password, String v3_privacy_protocol, String v3_privacy_password) {
        this(version, local_port, retries, timeout, v2_community, v3_security_name, v3_security_level, v3_security_engine_id, v3_context, v3_context_engine_id, v3_authentication_protocol, v3_authentication_password, v3_privacy_protocol, v3_privacy_password, null, null, null);    }
    public SNMPCredentials(Integer version, Integer local_port, Integer retries, Integer timeout, String v2_community, String v3_security_name, String v3_security_level, String v3_security_engine_id, String v3_context, String v3_context_engine_id, String v3_authentication_protocol, String v3_authentication_password, String v3_privacy_protocol, String v3_privacy_password, String v3_engine_id) {
        this(version, local_port, retries, timeout, v2_community, v3_security_name, v3_security_level, v3_security_engine_id, v3_context, v3_context_engine_id, v3_authentication_protocol, v3_authentication_password, v3_privacy_protocol, v3_privacy_password, v3_engine_id, null, null);    }
    public SNMPCredentials(Integer version, Integer local_port, Integer retries, Integer timeout, String v2_community, String v3_security_name, String v3_security_level, String v3_security_engine_id, String v3_context, String v3_context_engine_id, String v3_authentication_protocol, String v3_authentication_password, String v3_privacy_protocol, String v3_privacy_password, String v3_engine_id, Integer v3_engine_boots) {
        this(version, local_port, retries, timeout, v2_community, v3_security_name, v3_security_level, v3_security_engine_id, v3_context, v3_context_engine_id, v3_authentication_protocol, v3_authentication_password, v3_privacy_protocol, v3_privacy_password, v3_engine_id, v3_engine_boots, null);    }
    
    public Integer getVersion() {
        return version;
    }
    
    public void setVersion(Integer version) {
        this.version = version;
    }
    
    
    public Integer getLocalPort() {
        return local_port;
    }
    
    public void setLocalPort(Integer local_port) {
        this.local_port = local_port;
    }
    
    
    public Integer getRetries() {
        return retries;
    }
    
    public void setRetries(Integer retries) {
        this.retries = retries;
    }
    
    
    public Integer getTimeout() {
        return timeout;
    }
    
    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }
    
    
    public String getV2Community() {
        return v2_community;
    }
    
    public void setV2Community(String v2_community) {
        this.v2_community = v2_community;
    }
    
    
    public String getV3SecurityName() {
        return v3_security_name;
    }
    
    public void setV3SecurityName(String v3_security_name) {
        this.v3_security_name = v3_security_name;
    }
    
    
    public String getV3SecurityLevel() {
        return v3_security_level;
    }
    
    public void setV3SecurityLevel(String v3_security_level) {
        this.v3_security_level = v3_security_level;
    }
    
    
    public String getV3SecurityEngineId() {
        return v3_security_engine_id;
    }
    
    public void setV3SecurityEngineId(String v3_security_engine_id) {
        this.v3_security_engine_id = v3_security_engine_id;
    }
    
    
    public String getV3Context() {
        return v3_context;
    }
    
    public void setV3Context(String v3_context) {
        this.v3_context = v3_context;
    }
    
    
    public String getV3ContextEngineId() {
        return v3_context_engine_id;
    }
    
    public void setV3ContextEngineId(String v3_context_engine_id) {
        this.v3_context_engine_id = v3_context_engine_id;
    }
    
    
    public String getV3AuthenticationProtocol() {
        return v3_authentication_protocol;
    }
    
    public void setV3AuthenticationProtocol(String v3_authentication_protocol) {
        this.v3_authentication_protocol = v3_authentication_protocol;
    }
    
    
    public String getV3AuthenticationPassword() {
        return v3_authentication_password;
    }
    
    public void setV3AuthenticationPassword(String v3_authentication_password) {
        this.v3_authentication_password = v3_authentication_password;
    }
    
    
    public String getV3PrivacyProtocol() {
        return v3_privacy_protocol;
    }
    
    public void setV3PrivacyProtocol(String v3_privacy_protocol) {
        this.v3_privacy_protocol = v3_privacy_protocol;
    }
    
    
    public String getV3PrivacyPassword() {
        return v3_privacy_password;
    }
    
    public void setV3PrivacyPassword(String v3_privacy_password) {
        this.v3_privacy_password = v3_privacy_password;
    }
    
    
    public String getV3EngineId() {
        return v3_engine_id;
    }
    
    public void setV3EngineId(String v3_engine_id) {
        this.v3_engine_id = v3_engine_id;
    }
    
    
    public Integer getV3EngineBoots() {
        return v3_engine_boots;
    }
    
    public void setV3EngineBoots(Integer v3_engine_boots) {
        this.v3_engine_boots = v3_engine_boots;
    }
    
    
    public Integer getV3EngineTime() {
        return v3_engine_time;
    }
    
    public void setV3EngineTime(Integer v3_engine_time) {
        this.v3_engine_time = v3_engine_time;
    }
    
}
