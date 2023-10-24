//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class LoadbalancerHealthmonitorType extends ApiPropertyBase {
    Boolean admin_state;
    String monitor_type;
    Integer delay;
    Integer timeout;
    Integer max_retries;
    String http_method;
    String url_path;
    String expected_codes;
    public LoadbalancerHealthmonitorType() {
    }
    public LoadbalancerHealthmonitorType(Boolean admin_state, String monitor_type, Integer delay, Integer timeout, Integer max_retries, String http_method, String url_path, String expected_codes) {
        this.admin_state = admin_state;
        this.monitor_type = monitor_type;
        this.delay = delay;
        this.timeout = timeout;
        this.max_retries = max_retries;
        this.http_method = http_method;
        this.url_path = url_path;
        this.expected_codes = expected_codes;
    }
    public LoadbalancerHealthmonitorType(Boolean admin_state) {
        this(admin_state, null, null, null, null, null, null, null);    }
    public LoadbalancerHealthmonitorType(Boolean admin_state, String monitor_type) {
        this(admin_state, monitor_type, null, null, null, null, null, null);    }
    public LoadbalancerHealthmonitorType(Boolean admin_state, String monitor_type, Integer delay) {
        this(admin_state, monitor_type, delay, null, null, null, null, null);    }
    public LoadbalancerHealthmonitorType(Boolean admin_state, String monitor_type, Integer delay, Integer timeout) {
        this(admin_state, monitor_type, delay, timeout, null, null, null, null);    }
    public LoadbalancerHealthmonitorType(Boolean admin_state, String monitor_type, Integer delay, Integer timeout, Integer max_retries) {
        this(admin_state, monitor_type, delay, timeout, max_retries, null, null, null);    }
    public LoadbalancerHealthmonitorType(Boolean admin_state, String monitor_type, Integer delay, Integer timeout, Integer max_retries, String http_method) {
        this(admin_state, monitor_type, delay, timeout, max_retries, http_method, null, null);    }
    public LoadbalancerHealthmonitorType(Boolean admin_state, String monitor_type, Integer delay, Integer timeout, Integer max_retries, String http_method, String url_path) {
        this(admin_state, monitor_type, delay, timeout, max_retries, http_method, url_path, null);    }
    
    public Boolean getAdminState() {
        return admin_state;
    }
    
    public void setAdminState(Boolean admin_state) {
        this.admin_state = admin_state;
    }
    
    
    public String getMonitorType() {
        return monitor_type;
    }
    
    public void setMonitorType(String monitor_type) {
        this.monitor_type = monitor_type;
    }
    
    
    public Integer getDelay() {
        return delay;
    }
    
    public void setDelay(Integer delay) {
        this.delay = delay;
    }
    
    
    public Integer getTimeout() {
        return timeout;
    }
    
    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }
    
    
    public Integer getMaxRetries() {
        return max_retries;
    }
    
    public void setMaxRetries(Integer max_retries) {
        this.max_retries = max_retries;
    }
    
    
    public String getHttpMethod() {
        return http_method;
    }
    
    public void setHttpMethod(String http_method) {
        this.http_method = http_method;
    }
    
    
    public String getUrlPath() {
        return url_path;
    }
    
    public void setUrlPath(String url_path) {
        this.url_path = url_path;
    }
    
    
    public String getExpectedCodes() {
        return expected_codes;
    }
    
    public void setExpectedCodes(String expected_codes) {
        this.expected_codes = expected_codes;
    }
    
}
