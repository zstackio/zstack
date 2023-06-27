//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;
import com.google.common.collect.Lists;

import org.zstack.sugonSdnController.controller.api.ApiObjectBase;
import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;
import org.zstack.sugonSdnController.controller.api.ObjectReference;

public class JobTemplate extends ApiObjectBase {
    private Boolean job_template_synchronous_job;
    private String job_template_type;
    private String job_template_concurrency_level;
    private PlaybookInfoListType job_template_playbooks;
    private PlaybookInfoListType job_template_recovery_playbooks;
    private ExecutableInfoListType job_template_executables;
    private String job_template_input_schema;
    private String job_template_output_schema;
    private String job_template_input_ui_schema;
    private String job_template_output_ui_schema;
    private String job_template_description;
    private IdPermsType id_perms;
    private PermType2 perms2;
    private KeyValuePairs annotations;
    private String display_name;
    private List<ObjectReference<ApiPropertyBase>> tag_refs;
    private transient List<ObjectReference<ApiPropertyBase>> node_profile_back_refs;

    @Override
    public String getObjectType() {
        return "job-template";
    }

    @Override
    public List<String> getDefaultParent() {
        return Lists.newArrayList("default-global-system-config");
    }

    @Override
    public String getDefaultParentType() {
        return "global-system-config";
    }

    public void setParent(GlobalSystemConfig parent) {
        super.setParent(parent);
    }
    
    public Boolean getSynchronousJob() {
        return job_template_synchronous_job;
    }
    
    public void setSynchronousJob(Boolean job_template_synchronous_job) {
        this.job_template_synchronous_job = job_template_synchronous_job;
    }
    
    
    public String getType() {
        return job_template_type;
    }
    
    public void setType(String job_template_type) {
        this.job_template_type = job_template_type;
    }
    
    
    public String getConcurrencyLevel() {
        return job_template_concurrency_level;
    }
    
    public void setConcurrencyLevel(String job_template_concurrency_level) {
        this.job_template_concurrency_level = job_template_concurrency_level;
    }
    
    
    public PlaybookInfoListType getPlaybooks() {
        return job_template_playbooks;
    }
    
    public void setPlaybooks(PlaybookInfoListType job_template_playbooks) {
        this.job_template_playbooks = job_template_playbooks;
    }
    
    
    public PlaybookInfoListType getRecoveryPlaybooks() {
        return job_template_recovery_playbooks;
    }
    
    public void setRecoveryPlaybooks(PlaybookInfoListType job_template_recovery_playbooks) {
        this.job_template_recovery_playbooks = job_template_recovery_playbooks;
    }
    
    
    public ExecutableInfoListType getExecutables() {
        return job_template_executables;
    }
    
    public void setExecutables(ExecutableInfoListType job_template_executables) {
        this.job_template_executables = job_template_executables;
    }
    
    
    public String getInputSchema() {
        return job_template_input_schema;
    }
    
    public void setInputSchema(String job_template_input_schema) {
        this.job_template_input_schema = job_template_input_schema;
    }
    
    
    public String getOutputSchema() {
        return job_template_output_schema;
    }
    
    public void setOutputSchema(String job_template_output_schema) {
        this.job_template_output_schema = job_template_output_schema;
    }
    
    
    public String getInputUiSchema() {
        return job_template_input_ui_schema;
    }
    
    public void setInputUiSchema(String job_template_input_ui_schema) {
        this.job_template_input_ui_schema = job_template_input_ui_schema;
    }
    
    
    public String getOutputUiSchema() {
        return job_template_output_ui_schema;
    }
    
    public void setOutputUiSchema(String job_template_output_ui_schema) {
        this.job_template_output_ui_schema = job_template_output_ui_schema;
    }
    
    
    public String getDescription() {
        return job_template_description;
    }
    
    public void setDescription(String job_template_description) {
        this.job_template_description = job_template_description;
    }
    
    
    public IdPermsType getIdPerms() {
        return id_perms;
    }
    
    public void setIdPerms(IdPermsType id_perms) {
        this.id_perms = id_perms;
    }
    
    
    public PermType2 getPerms2() {
        return perms2;
    }
    
    public void setPerms2(PermType2 perms2) {
        this.perms2 = perms2;
    }
    
    
    public KeyValuePairs getAnnotations() {
        return annotations;
    }
    
    public void setAnnotations(KeyValuePairs annotations) {
        this.annotations = annotations;
    }
    
    
    public String getDisplayName() {
        return display_name;
    }
    
    public void setDisplayName(String display_name) {
        this.display_name = display_name;
    }
    

    public List<ObjectReference<ApiPropertyBase>> getTag() {
        return tag_refs;
    }

    public void setTag(Tag obj) {
        tag_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        tag_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void addTag(Tag obj) {
        if (tag_refs == null) {
            tag_refs = new ArrayList<ObjectReference<ApiPropertyBase>>();
        }
        tag_refs.add(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
    }
    
    public void removeTag(Tag obj) {
        if (tag_refs != null) {
            tag_refs.remove(new ObjectReference<ApiPropertyBase>(obj.getQualifiedName(), null));
        }
    }

    public void clearTag() {
        if (tag_refs != null) {
            tag_refs.clear();
            return;
        }
        tag_refs = null;
    }

    public List<ObjectReference<ApiPropertyBase>> getNodeProfileBackRefs() {
        return node_profile_back_refs;
    }
}