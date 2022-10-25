//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class PlaybookInfoType extends ApiPropertyBase {
    String playbook_uri;
    Boolean multi_device_playbook;
    String vendor;
    String device_family;
    Integer job_completion_weightage;
    Integer sequence_no;
    public PlaybookInfoType() {
    }
    public PlaybookInfoType(String playbook_uri, Boolean multi_device_playbook, String vendor, String device_family, Integer job_completion_weightage, Integer sequence_no) {
        this.playbook_uri = playbook_uri;
        this.multi_device_playbook = multi_device_playbook;
        this.vendor = vendor;
        this.device_family = device_family;
        this.job_completion_weightage = job_completion_weightage;
        this.sequence_no = sequence_no;
    }
    public PlaybookInfoType(String playbook_uri) {
        this(playbook_uri, false, null, null, 100, null);    }
    public PlaybookInfoType(String playbook_uri, Boolean multi_device_playbook) {
        this(playbook_uri, multi_device_playbook, null, null, 100, null);    }
    public PlaybookInfoType(String playbook_uri, Boolean multi_device_playbook, String vendor) {
        this(playbook_uri, multi_device_playbook, vendor, null, 100, null);    }
    public PlaybookInfoType(String playbook_uri, Boolean multi_device_playbook, String vendor, String device_family) {
        this(playbook_uri, multi_device_playbook, vendor, device_family, 100, null);    }
    public PlaybookInfoType(String playbook_uri, Boolean multi_device_playbook, String vendor, String device_family, Integer job_completion_weightage) {
        this(playbook_uri, multi_device_playbook, vendor, device_family, job_completion_weightage, null);    }
    
    public String getPlaybookUri() {
        return playbook_uri;
    }
    
    public void setPlaybookUri(String playbook_uri) {
        this.playbook_uri = playbook_uri;
    }
    
    
    public Boolean getMultiDevicePlaybook() {
        return multi_device_playbook;
    }
    
    public void setMultiDevicePlaybook(Boolean multi_device_playbook) {
        this.multi_device_playbook = multi_device_playbook;
    }
    
    
    public String getVendor() {
        return vendor;
    }
    
    public void setVendor(String vendor) {
        this.vendor = vendor;
    }
    
    
    public String getDeviceFamily() {
        return device_family;
    }
    
    public void setDeviceFamily(String device_family) {
        this.device_family = device_family;
    }
    
    
    public Integer getJobCompletionWeightage() {
        return job_completion_weightage;
    }
    
    public void setJobCompletionWeightage(Integer job_completion_weightage) {
        this.job_completion_weightage = job_completion_weightage;
    }
    
    
    public Integer getSequenceNo() {
        return sequence_no;
    }
    
    public void setSequenceNo(Integer sequence_no) {
        this.sequence_no = sequence_no;
    }
    
}
