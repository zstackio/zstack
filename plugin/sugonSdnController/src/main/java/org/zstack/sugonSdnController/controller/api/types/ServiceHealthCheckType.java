//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class ServiceHealthCheckType extends ApiPropertyBase {
    Boolean enabled;
    String health_check_type;
    String monitor_type;
    Integer delay;
    Integer delayUsecs;
    Integer timeout;
    Integer timeoutUsecs;
    Integer max_retries;
    String http_method;
    String url_path;
    String expected_codes;
    Boolean target_ip_all;
    IpAddressesType target_ip_list;
    public ServiceHealthCheckType() {
    }
    public ServiceHealthCheckType(Boolean enabled, String health_check_type, String monitor_type, Integer delay, Integer delayUsecs, Integer timeout, Integer timeoutUsecs, Integer max_retries, String http_method, String url_path, String expected_codes, Boolean target_ip_all, IpAddressesType target_ip_list) {
        this.enabled = enabled;
        this.health_check_type = health_check_type;
        this.monitor_type = monitor_type;
        this.delay = delay;
        this.delayUsecs = delayUsecs;
        this.timeout = timeout;
        this.timeoutUsecs = timeoutUsecs;
        this.max_retries = max_retries;
        this.http_method = http_method;
        this.url_path = url_path;
        this.expected_codes = expected_codes;
        this.target_ip_all = target_ip_all;
        this.target_ip_list = target_ip_list;
    }
    public ServiceHealthCheckType(Boolean enabled) {
        this(enabled, null, null, null, 0, null, 0, null, null, null, null, false, null);    }
    public ServiceHealthCheckType(Boolean enabled, String health_check_type) {
        this(enabled, health_check_type, null, null, 0, null, 0, null, null, null, null, false, null);    }
    public ServiceHealthCheckType(Boolean enabled, String health_check_type, String monitor_type) {
        this(enabled, health_check_type, monitor_type, null, 0, null, 0, null, null, null, null, false, null);    }
    public ServiceHealthCheckType(Boolean enabled, String health_check_type, String monitor_type, Integer delay) {
        this(enabled, health_check_type, monitor_type, delay, 0, null, 0, null, null, null, null, false, null);    }
    public ServiceHealthCheckType(Boolean enabled, String health_check_type, String monitor_type, Integer delay, Integer delayUsecs) {
        this(enabled, health_check_type, monitor_type, delay, delayUsecs, null, 0, null, null, null, null, false, null);    }
    public ServiceHealthCheckType(Boolean enabled, String health_check_type, String monitor_type, Integer delay, Integer delayUsecs, Integer timeout) {
        this(enabled, health_check_type, monitor_type, delay, delayUsecs, timeout, 0, null, null, null, null, false, null);    }
    public ServiceHealthCheckType(Boolean enabled, String health_check_type, String monitor_type, Integer delay, Integer delayUsecs, Integer timeout, Integer timeoutUsecs) {
        this(enabled, health_check_type, monitor_type, delay, delayUsecs, timeout, timeoutUsecs, null, null, null, null, false, null);    }
    public ServiceHealthCheckType(Boolean enabled, String health_check_type, String monitor_type, Integer delay, Integer delayUsecs, Integer timeout, Integer timeoutUsecs, Integer max_retries) {
        this(enabled, health_check_type, monitor_type, delay, delayUsecs, timeout, timeoutUsecs, max_retries, null, null, null, false, null);    }
    public ServiceHealthCheckType(Boolean enabled, String health_check_type, String monitor_type, Integer delay, Integer delayUsecs, Integer timeout, Integer timeoutUsecs, Integer max_retries, String http_method) {
        this(enabled, health_check_type, monitor_type, delay, delayUsecs, timeout, timeoutUsecs, max_retries, http_method, null, null, false, null);    }
    public ServiceHealthCheckType(Boolean enabled, String health_check_type, String monitor_type, Integer delay, Integer delayUsecs, Integer timeout, Integer timeoutUsecs, Integer max_retries, String http_method, String url_path) {
        this(enabled, health_check_type, monitor_type, delay, delayUsecs, timeout, timeoutUsecs, max_retries, http_method, url_path, null, false, null);    }
    public ServiceHealthCheckType(Boolean enabled, String health_check_type, String monitor_type, Integer delay, Integer delayUsecs, Integer timeout, Integer timeoutUsecs, Integer max_retries, String http_method, String url_path, String expected_codes) {
        this(enabled, health_check_type, monitor_type, delay, delayUsecs, timeout, timeoutUsecs, max_retries, http_method, url_path, expected_codes, false, null);    }
    public ServiceHealthCheckType(Boolean enabled, String health_check_type, String monitor_type, Integer delay, Integer delayUsecs, Integer timeout, Integer timeoutUsecs, Integer max_retries, String http_method, String url_path, String expected_codes, Boolean target_ip_all) {
        this(enabled, health_check_type, monitor_type, delay, delayUsecs, timeout, timeoutUsecs, max_retries, http_method, url_path, expected_codes, target_ip_all, null);    }
    
    public Boolean getEnabled() {
        return enabled;
    }
    
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
    
    
    public String getHealthCheckType() {
        return health_check_type;
    }
    
    public void setHealthCheckType(String health_check_type) {
        this.health_check_type = health_check_type;
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
    
    
    public Integer getDelayusecs() {
        return delayUsecs;
    }
    
    public void setDelayusecs(Integer delayUsecs) {
        this.delayUsecs = delayUsecs;
    }
    
    
    public Integer getTimeout() {
        return timeout;
    }
    
    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }
    
    
    public Integer getTimeoutusecs() {
        return timeoutUsecs;
    }
    
    public void setTimeoutusecs(Integer timeoutUsecs) {
        this.timeoutUsecs = timeoutUsecs;
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
    
    
    public Boolean getTargetIpAll() {
        return target_ip_all;
    }
    
    public void setTargetIpAll(Boolean target_ip_all) {
        this.target_ip_all = target_ip_all;
    }
    
    
    public IpAddressesType getTargetIpList() {
        return target_ip_list;
    }
    
    public void setTargetIpList(IpAddressesType target_ip_list) {
        this.target_ip_list = target_ip_list;
    }
    
}
