package org.zstack.header.identity;

import org.springframework.security.access.ConfigAttribute;

import java.util.List;

public class AuthorizationInfo implements ConfigAttribute {
    private List<String> needRoles;

    public List<String> getNeedRoles() {
        return needRoles;
    }

    public void setNeedRoles(List<String> needRoles) {
        this.needRoles = needRoles;
    }

    @Override
    public String getAttribute() {
        return null;
    }
}
