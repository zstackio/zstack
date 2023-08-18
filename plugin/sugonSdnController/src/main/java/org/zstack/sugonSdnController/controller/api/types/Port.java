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

public class Port extends ApiObjectBase {
    private String port_group_uuid;
    private BaremetalPortInfo bms_port_info;
    private ESXIProperties esxi_port_info;
    private String label;
    private IdPermsType id_perms;
    private PermType2 perms2;
    private KeyValuePairs annotations;
    private String display_name;
    private List<ObjectReference<ApiPropertyBase>> tag_refs;
    private transient List<ObjectReference<ApiPropertyBase>> port_group_back_refs;
    private transient List<ObjectReference<ApiPropertyBase>> physical_interface_back_refs;

    @Override
    public String getObjectType() {
        return "port";
    }

    @Override
    public List<String> getDefaultParent() {
        return Lists.newArrayList("default-global-system-config", "default-node");
    }

    @Override
    public String getDefaultParentType() {
        return "node";
    }

    public void setParent(Node parent) {
        super.setParent(parent);
    }
    
    public String getGroupUuid() {
        return port_group_uuid;
    }
    
    public void setGroupUuid(String port_group_uuid) {
        this.port_group_uuid = port_group_uuid;
    }
    
    
    public BaremetalPortInfo getBmsPortInfo() {
        return bms_port_info;
    }
    
    public void setBmsPortInfo(BaremetalPortInfo bms_port_info) {
        this.bms_port_info = bms_port_info;
    }
    
    
    public ESXIProperties getEsxiPortInfo() {
        return esxi_port_info;
    }
    
    public void setEsxiPortInfo(ESXIProperties esxi_port_info) {
        this.esxi_port_info = esxi_port_info;
    }
    
    
    public String getLabel() {
        return label;
    }
    
    public void setLabel(String label) {
        this.label = label;
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

    public List<ObjectReference<ApiPropertyBase>> getPortGroupBackRefs() {
        return port_group_back_refs;
    }

    public List<ObjectReference<ApiPropertyBase>> getPhysicalInterfaceBackRefs() {
        return physical_interface_back_refs;
    }
}