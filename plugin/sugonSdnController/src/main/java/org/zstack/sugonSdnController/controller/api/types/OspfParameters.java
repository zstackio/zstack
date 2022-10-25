//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class OspfParameters extends ApiPropertyBase {
    String auth_data;
    Integer hello_interval;
    Integer dead_interval;
    String area_id;
    String area_type;
    Boolean advertise_loopback;
    Boolean orignate_summary_lsa;
    public OspfParameters() {
    }
    public OspfParameters(String auth_data, Integer hello_interval, Integer dead_interval, String area_id, String area_type, Boolean advertise_loopback, Boolean orignate_summary_lsa) {
        this.auth_data = auth_data;
        this.hello_interval = hello_interval;
        this.dead_interval = dead_interval;
        this.area_id = area_id;
        this.area_type = area_type;
        this.advertise_loopback = advertise_loopback;
        this.orignate_summary_lsa = orignate_summary_lsa;
    }
    public OspfParameters(String auth_data) {
        this(auth_data, 10, 40, null, null, null, null);    }
    public OspfParameters(String auth_data, Integer hello_interval) {
        this(auth_data, hello_interval, 40, null, null, null, null);    }
    public OspfParameters(String auth_data, Integer hello_interval, Integer dead_interval) {
        this(auth_data, hello_interval, dead_interval, null, null, null, null);    }
    public OspfParameters(String auth_data, Integer hello_interval, Integer dead_interval, String area_id) {
        this(auth_data, hello_interval, dead_interval, area_id, null, null, null);    }
    public OspfParameters(String auth_data, Integer hello_interval, Integer dead_interval, String area_id, String area_type) {
        this(auth_data, hello_interval, dead_interval, area_id, area_type, null, null);    }
    public OspfParameters(String auth_data, Integer hello_interval, Integer dead_interval, String area_id, String area_type, Boolean advertise_loopback) {
        this(auth_data, hello_interval, dead_interval, area_id, area_type, advertise_loopback, null);    }
    
    public String getAuthData() {
        return auth_data;
    }
    
    public void setAuthData(String auth_data) {
        this.auth_data = auth_data;
    }
    
    
    public Integer getHelloInterval() {
        return hello_interval;
    }
    
    public void setHelloInterval(Integer hello_interval) {
        this.hello_interval = hello_interval;
    }
    
    
    public Integer getDeadInterval() {
        return dead_interval;
    }
    
    public void setDeadInterval(Integer dead_interval) {
        this.dead_interval = dead_interval;
    }
    
    
    public String getAreaId() {
        return area_id;
    }
    
    public void setAreaId(String area_id) {
        this.area_id = area_id;
    }
    
    
    public String getAreaType() {
        return area_type;
    }
    
    public void setAreaType(String area_type) {
        this.area_type = area_type;
    }
    
    
    public Boolean getAdvertiseLoopback() {
        return advertise_loopback;
    }
    
    public void setAdvertiseLoopback(Boolean advertise_loopback) {
        this.advertise_loopback = advertise_loopback;
    }
    
    
    public Boolean getOrignateSummaryLsa() {
        return orignate_summary_lsa;
    }
    
    public void setOrignateSummaryLsa(Boolean orignate_summary_lsa) {
        this.orignate_summary_lsa = orignate_summary_lsa;
    }
    
}
