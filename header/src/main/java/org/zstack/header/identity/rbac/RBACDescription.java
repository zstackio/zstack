package org.zstack.header.identity.rbac;

import java.util.*;

public interface RBACDescription {
    List<Permission> permissions = new ArrayList<>();
    List<RoleContributor> roleContributors = new ArrayList<>();

    class Permission {
        private Set<String> adminOnlyAPIs = new HashSet<>();
        private Set<String> normalAPIs = new HashSet<>();
        private List<Class> targetResources = new ArrayList<>();
        private Set<String> _adminOnlyAPIs = new HashSet<>();
        private Set<String> _normalAPIs = new HashSet<>();
        private String name;

        public Set<String> getAdminOnlyAPIs() {
            return adminOnlyAPIs;
        }

        public void setAdminOnlyAPIs(Set<String> adminOnlyAPIs) {
            this.adminOnlyAPIs = adminOnlyAPIs;
        }

        public Set<String> getNormalAPIs() {
            return normalAPIs;
        }

        public void setNormalAPIs(Set<String> normalAPIs) {
            this.normalAPIs = normalAPIs;
        }

        public List<Class> getTargetResources() {
            return targetResources;
        }

        public void setTargetResources(List<Class> targetResources) {
            this.targetResources = targetResources;
        }

        public Set<String> get_adminOnlyAPIs() {
            return _adminOnlyAPIs;
        }

        public void set_adminOnlyAPIs(Set<String> _adminOnlyAPIs) {
            this._adminOnlyAPIs = _adminOnlyAPIs;
        }

        public Set<String> get_normalAPIs() {
            return _normalAPIs;
        }

        public void set_normalAPIs(Set<String> _normalAPIs) {
            this._normalAPIs = _normalAPIs;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    class PermissionBuilder {
        Permission permission = new Permission();

        public PermissionBuilder name(String v) {
            permission.setName(v);
            return this;
        }

        public PermissionBuilder normalAPIs(String...vs) {
            for (String v : vs) {
                permission.get_normalAPIs().add(v);
            }

            return this;
        }

        public PermissionBuilder adminOnlyAPIs(String...vs) {
            for (String v : vs) {
                permission.get_adminOnlyAPIs().add(v);
            }

            return this;
        }

        public PermissionBuilder normalAPIs(Class...clzs) {
            for (Class clz : clzs) {
                permission.getNormalAPIs().add(clz.getName());
            }

            return this;
        }

        public PermissionBuilder adminOnlyAPIs(Class...clzs) {
            for (Class clz : clzs) {
                permission.get_adminOnlyAPIs().add(clz.getName());
            }

            return this;
        }

        public PermissionBuilder targetResources(Class...clzs) {
            for (Class clz : clzs) {
                permission.getTargetResources().add(clz);
            }

            return this;
        }

        public Permission build() {
            permission = RBACDescriptionHelper.flatten(permission);
            permissions.add(permission);
            return permission;
        }
    }

    class RoleContributor {
        private List<String> normalActionsByPermissionName = new ArrayList<>();
        private List<String> actions = new ArrayList<>();
        private String roleName;

        public List<String> getNormalActionsByPermissionName() {
            return normalActionsByPermissionName;
        }

        public void setNormalActionsByPermissionName(List<String> normalActionsByPermissionName) {
            this.normalActionsByPermissionName = normalActionsByPermissionName;
        }

        public List<String> getActions() {
            return actions;
        }

        public void setActions(List<String> actions) {
            this.actions = actions;
        }

        public String getRoleName() {
            return roleName;
        }

        public void setRoleName(String roleName) {
            this.roleName = roleName;
        }
    }

    class RoleContributorBuilder {
        private RoleContributor contributor;

        public RoleContributorBuilder actionsByPermissionName(String v) {
            contributor.normalActionsByPermissionName.add(v);
            return this;
        }

        public RoleContributorBuilder actions(String...vs) {
            contributor.actions.addAll(Arrays.asList(vs));
            return this;
        }

        public RoleContributorBuilder actions(Class...clzs) {
            for (Class clz : clzs) {
                contributor.actions.add(clz.getName());
            }
            return this;
        }

        public RoleContributorBuilder roleName(String v) {
            contributor.roleName = v;
            return this;
        }

        public RoleContributor build() {
            roleContributors.add(contributor);
            return contributor;
        }
    }

    default PermissionBuilder permissionBuilder() {
        return new PermissionBuilder();
    }


    default RoleContributorBuilder roleContributorBuilder() {
        return new RoleContributorBuilder();
    }

    void permissions();

    void contributeToRoles();

    default RBACEntityFormatter entityFormatter() {
        return null;
    }

    default Class[] globalReadableResources() {
        return new Class[] {};
    }
}
