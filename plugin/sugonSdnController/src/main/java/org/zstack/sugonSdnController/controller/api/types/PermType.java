//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class PermType extends ApiPropertyBase {
    String owner;
    Integer owner_access;
    String group;
    Integer group_access;
    Integer other_access;
    public PermType() {
    }
    public PermType(String owner, Integer owner_access, String group, Integer group_access, Integer other_access) {
        this.owner = owner;
        this.owner_access = owner_access;
        this.group = group;
        this.group_access = group_access;
        this.other_access = other_access;
    }
    public PermType(String owner) {
        this(owner, null, null, null, null);    }
    public PermType(String owner, Integer owner_access) {
        this(owner, owner_access, null, null, null);    }
    public PermType(String owner, Integer owner_access, String group) {
        this(owner, owner_access, group, null, null);    }
    public PermType(String owner, Integer owner_access, String group, Integer group_access) {
        this(owner, owner_access, group, group_access, null);    }
    
    public String getOwner() {
        return owner;
    }
    
    public void setOwner(String owner) {
        this.owner = owner;
    }
    
    
    public Integer getOwnerAccess() {
        return owner_access;
    }
    
    public void setOwnerAccess(Integer owner_access) {
        this.owner_access = owner_access;
    }
    
    
    public String getGroup() {
        return group;
    }
    
    public void setGroup(String group) {
        this.group = group;
    }
    
    
    public Integer getGroupAccess() {
        return group_access;
    }
    
    public void setGroupAccess(Integer group_access) {
        this.group_access = group_access;
    }
    
    
    public Integer getOtherAccess() {
        return other_access;
    }
    
    public void setOtherAccess(Integer other_access) {
        this.other_access = other_access;
    }
    
}
