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

public class Domain extends ApiObjectBase {
    private DomainLimitsType domain_limits;
    private IdPermsType id_perms;
    private PermType2 perms2;
    private KeyValuePairs annotations;
    private String display_name;
    private List<ObjectReference<ApiPropertyBase>> projects;
    private List<ObjectReference<ApiPropertyBase>> namespaces;
    private List<ObjectReference<ApiPropertyBase>> service_templates;
    private List<ObjectReference<ApiPropertyBase>> virtual_DNSs;
    private List<ObjectReference<ApiPropertyBase>> api_access_lists;
    private List<ObjectReference<ApiPropertyBase>> tag_refs;

    @Override
    public String getObjectType() {
        return "domain";
    }

    @Override
    public List<String> getDefaultParent() {
        return Lists.newArrayList();
    }

    @Override
    public String getDefaultParentType() {
        return "config-root";
    }

    public void setParent(ConfigRoot parent) {
        super.setParent(parent);
    }
    
    public DomainLimitsType getLimits() {
        return domain_limits;
    }
    
    public void setLimits(DomainLimitsType domain_limits) {
        this.domain_limits = domain_limits;
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
    

    public List<ObjectReference<ApiPropertyBase>> getProjects() {
        return projects;
    }

    public List<ObjectReference<ApiPropertyBase>> getNamespaces() {
        return namespaces;
    }

    public List<ObjectReference<ApiPropertyBase>> getServiceTemplates() {
        return service_templates;
    }

    public List<ObjectReference<ApiPropertyBase>> getVirtualDnss() {
        return virtual_DNSs;
    }

    public List<ObjectReference<ApiPropertyBase>> getApiAccessLists() {
        return api_access_lists;
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
}