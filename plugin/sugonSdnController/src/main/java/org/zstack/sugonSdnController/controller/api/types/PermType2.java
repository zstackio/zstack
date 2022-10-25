//
// Automatically generated.
//
package org.zstack.sugonSdnController.controller.api.types;

import java.util.List;
import java.util.ArrayList;

import org.zstack.sugonSdnController.controller.api.ApiPropertyBase;

public class PermType2 extends ApiPropertyBase {
    String owner;
    Integer owner_access;
    Integer global_access;
    List<ShareType> share;
    public PermType2() {
    }
    public PermType2(String owner, Integer owner_access, Integer global_access, List<ShareType> share) {
        this.owner = owner;
        this.owner_access = owner_access;
        this.global_access = global_access;
        this.share = share;
    }
    public PermType2(String owner) {
        this(owner, 7, 0, null);    }
    public PermType2(String owner, Integer owner_access) {
        this(owner, owner_access, 0, null);    }
    public PermType2(String owner, Integer owner_access, Integer global_access) {
        this(owner, owner_access, global_access, null);    }
    
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
    
    
    public Integer getGlobalAccess() {
        return global_access;
    }
    
    public void setGlobalAccess(Integer global_access) {
        this.global_access = global_access;
    }
    
    
    public List<ShareType> getShare() {
        return share;
    }
    
    
    public void addShare(ShareType obj) {
        if (share == null) {
            share = new ArrayList<ShareType>();
        }
        share.add(obj);
    }
    public void clearShare() {
        share = null;
    }
    
    
    public void addShare(String tenant, Integer tenant_access) {
        if (share == null) {
            share = new ArrayList<ShareType>();
        }
        share.add(new ShareType(tenant, tenant_access));
    }
    
}
