package org.zstack.header.identity.role;

import java.util.*;

public class RoleIdentity {
    private static Map<String, RoleIdentity> types = Collections.synchronizedMap(new HashMap<>());
    private final String identityName;
    protected List<RoleIdentityValidator> roleIdentityValidators = new ArrayList<>();

    public RoleIdentity(String identityName) {
        this.identityName = identityName;
        types.put(identityName, this);
    }

    public static boolean hasType(String type) {
        return types.keySet().contains(type);
    }

    public static RoleIdentity valueOf(String typeName) {
        RoleIdentity type = types.get(typeName);
        if (type == null) {
            throw new IllegalArgumentException("Role identity: " + typeName + "if not found");
        }
        return type;
    }

    @Override
    public String toString() {
        return identityName;
    }

    @Override
    public boolean equals(Object t) {
        if (t == null || !(t instanceof RoleIdentity)) {
            return false;
        }

        RoleIdentity type = (RoleIdentity) t;
        return type.toString().equals(identityName);
    }

    @Override
    public int hashCode() {
        return identityName.hashCode();
    }

    public RoleIdentity installRoleIdentityValidator(RoleIdentityValidator validator) {
        roleIdentityValidators.add(validator);
        return this;
    }

    public List<RoleIdentityValidator> getRoleIdentityValidators() {
        return roleIdentityValidators;
    }
}
