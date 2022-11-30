//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class IdPermsType extends ApiPropertyBase {
    PermType permissions;
    UuidType uuid;
    Boolean enable;
    volatile java.util.Date created;
    volatile java.util.Date last_modified;
    String description;
    Boolean user_visible;
    String creator;
    public IdPermsType() {
    }
    public IdPermsType(PermType permissions, UuidType uuid, Boolean enable, java.util.Date created, java.util.Date last_modified, String description, Boolean user_visible, String creator) {
        this.permissions = permissions;
        this.uuid = uuid;
        this.enable = enable;
        this.created = created;
        this.last_modified = last_modified;
        this.description = description;
        this.user_visible = user_visible;
        this.creator = creator;
    }
    public IdPermsType(PermType permissions) {
        this(permissions, null, null, null, null, null, true, null);    }
    public IdPermsType(PermType permissions, UuidType uuid) {
        this(permissions, uuid, null, null, null, null, true, null);    }
    public IdPermsType(PermType permissions, UuidType uuid, Boolean enable) {
        this(permissions, uuid, enable, null, null, null, true, null);    }
    public IdPermsType(PermType permissions, UuidType uuid, Boolean enable, java.util.Date created) {
        this(permissions, uuid, enable, created, null, null, true, null);    }
    public IdPermsType(PermType permissions, UuidType uuid, Boolean enable, java.util.Date created, java.util.Date last_modified) {
        this(permissions, uuid, enable, created, last_modified, null, true, null);    }
    public IdPermsType(PermType permissions, UuidType uuid, Boolean enable, java.util.Date created, java.util.Date last_modified, String description) {
        this(permissions, uuid, enable, created, last_modified, description, true, null);    }
    public IdPermsType(PermType permissions, UuidType uuid, Boolean enable, java.util.Date created, java.util.Date last_modified, String description, Boolean user_visible) {
        this(permissions, uuid, enable, created, last_modified, description, user_visible, null);    }
    
    public PermType getPermissions() {
        return permissions;
    }
    
    public void setPermissions(PermType permissions) {
        this.permissions = permissions;
    }
    
    
    public UuidType getUuid() {
        return uuid;
    }
    
    public void setUuid(UuidType uuid) {
        this.uuid = uuid;
    }
    
    
    public Boolean getEnable() {
        return enable;
    }
    
    public void setEnable(Boolean enable) {
        this.enable = enable;
    }
    
    
    public java.util.Date getCreated() {
        return created;
    }
    
    public void setCreated(java.util.Date created) {
        this.created = created;
    }
    
    
    public java.util.Date getLastModified() {
        return last_modified;
    }
    
    public void setLastModified(java.util.Date last_modified) {
        this.last_modified = last_modified;
    }
    
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    
    public Boolean getUserVisible() {
        return user_visible;
    }
    
    public void setUserVisible(Boolean user_visible) {
        this.user_visible = user_visible;
    }
    
    
    public String getCreator() {
        return creator;
    }
    
    public void setCreator(String creator) {
        this.creator = creator;
    }
    
}
