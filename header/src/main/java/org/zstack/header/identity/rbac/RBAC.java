package org.zstack.header.identity.rbac;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.core.StaticInit;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.PolicyStatement;
import org.zstack.header.identity.StatementEffect;
import org.zstack.header.identity.SuppressCredentialCheck;
import org.zstack.header.message.APIMessage;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.DebugUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class RBAC {
    public static List<Permission> permissions = new ArrayList<>();
    public static List<Role> roles = new ArrayList<>();
    public static List<GlobalReadableResource> readableResources = new ArrayList<>();
    public static Map<Class, List<APIPermissionCheckerWrapper>> permissionCheckers = new HashMap<>();
    private static Map<Class, List<RBACEntityFormatter>> entityFormatters = new HashMap<>();

    public static Map<Class, List<ExpendedFieldPermission>> expendApiClassForPermissionCheck = new HashMap<>();

    private static List<RoleContributor> roleContributors = new ArrayList<>();
    private static List<RoleBuilder> roleBuilders = new ArrayList<>();

    private static PolicyMatcher matcher = new PolicyMatcher();

    static class APIPermissionCheckerWrapper {
        boolean takeOver;
        APIPermissionChecker checker;
    }

    public static void checkMergeTo() {
        DebugUtils.Assert(permissions.stream().anyMatch(p -> p.mergeTo != null), "No RBAC permission has mergeTo field");

        permissions.forEach(p -> {
            if (p.mergeTo != null) {
                Optional<Permission> opt = permissions.stream().filter(it -> it.name != null && it.name.equals(p.mergeTo)).findFirst();
                if (!opt.isPresent()) {
                    throw new CloudRuntimeException(String.format("cannot find permission[name:%s] to merge to", p.mergeTo));
                }

                Permission target = opt.get();

                DebugUtils.Assert(target.getAdminOnlyAPIs().containsAll(p.getAdminOnlyAPIs()), String.format("permission[name:%s] merge to permission[name:%s], but adminOnlyAPIs not match", p.name, target.name));
                DebugUtils.Assert(target.getNormalAPIs().containsAll(p.getNormalAPIs()), String.format("permission[name:%s] merge to permission[name:%s], but normalAPIs not match", p.name, target.name));
                DebugUtils.Assert(new HashSet<>(target.getTargetResources()).containsAll(p.getTargetResources()), String.format("permission[name:%s] merge to permission[name:%s], but targetResources not match", p.name, target.name));
            }
        });
    }

    public static void checkMissingRBACInfo() {
        PolicyMatcher matcher = new PolicyMatcher();

        List<String> missingInPermission = new ArrayList<>();
        List<String> missingInRole = new ArrayList<>();

        APIMessage.apiMessageClasses.forEach(clz -> {
            if (clz.isAnnotationPresent(Deprecated.class) || clz.isAnnotationPresent(SuppressCredentialCheck.class)) {
                return;
            }

            String clzName = clz.getName();
            boolean has = permissions.parallelStream()
                    .anyMatch(p -> p.normalAPIs.stream().anyMatch(s -> matcher.match(s, clzName)) || p.adminOnlyAPIs.stream().anyMatch(s -> matcher.match(s, clzName)));

            if (!has) {
                missingInPermission.add(clzName);
            }

            has = roles.parallelStream().anyMatch(r -> r.allowedActions.parallelStream().anyMatch(ac -> matcher.match(ac, clzName)) || r.excludedActions.parallelStream().anyMatch(ac -> matcher.match(ac, clzName)));
            // admin api won't belong to any role
            if (!has && !isAdminOnlyAPI(clzName)) {
                missingInRole.add(clzName);
            }
        });

        Collections.sort(missingInPermission);
        Collections.sort(missingInRole);
        if (missingInPermission.isEmpty() && missingInRole.isEmpty()) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        if (!missingInPermission.isEmpty()) {
            sb.append(String.format("Below APIs:\n %s not referred in any RBACInfo's permission\n", StringUtils.join(missingInPermission, "\n")));
        }

        if (!missingInRole.isEmpty()) {
            sb.append(String.format("Below APIs:\n %s not referred in any RBACInfo's role\n", StringUtils.join(missingInRole, "\n")));
        }

        throw new CloudRuntimeException(sb.toString());
    }

    public static class RoleBuilder {
        private Role role = new Role();
        private List<String> permissionsByNames = new ArrayList<>();

        {
            role.setPredefine(true);
        }

        public RoleBuilder uuid(String v) {
            role.uuid = v;
            return this;
        }

        public RoleBuilder name(String v) {
            role.name = v;
            return this;
        }

        public RoleBuilder actions(String...vs) {
            role.allowedActions.addAll(Arrays.asList(vs));
            return this;
        }

        public RoleBuilder actions(Class...clzs) {
            for (Class clz : clzs) {
                role.allowedActions.add(clz.getName());
            }
            return this;
        }

        public RoleBuilder permissionsByName(String...pnames) {
            permissionsByNames.addAll(Arrays.asList(pnames));
            return this;
        }

        public RoleBuilder allow() {
            role.effect = StatementEffect.Allow;
            return this;
        }

        public RoleBuilder deny() {
            role.effect = StatementEffect.Deny;
            return this;
        }

        public RoleBuilder predefined() {
            role.predefine = true;
            return this;
        }

        public RoleBuilder notPredefined() {
            role.predefine = false;
            return this;
        }

        public RoleBuilder excludeActions(String...vs) {
            for (String v : vs) {
                role.getExcludedActions().add(v);
            }
            return this;
        }

        public RoleBuilder excludeActions(Class...clzs) {
            for (Class clz : clzs) {
                role.getExcludedActions().add(clz.getName());
            }
            return this;
        }

        public void build() {
            roleBuilders.add(this);
        }
    }

    public static class Role {
        private String uuid;
        private String name;
        private Set<String> allowedActions = new HashSet<>();
        private StatementEffect effect = StatementEffect.Allow;
        private boolean adminOnly;
        private boolean predefine = true;
        private List<String> excludedActions = new ArrayList<>();

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Set<String> getAllowedActions() {
            return allowedActions;
        }

        public void setAllowedActions(Set<String> allowedActions) {
            this.allowedActions = allowedActions;
        }

        public StatementEffect getEffect() {
            return effect;
        }

        public void setEffect(StatementEffect effect) {
            this.effect = effect;
        }

        public boolean isAdminOnly() {
            return adminOnly;
        }

        public void setAdminOnly(boolean adminOnly) {
            this.adminOnly = adminOnly;
        }

        public boolean isPredefine() {
            return predefine;
        }

        public void setPredefine(boolean predefine) {
            this.predefine = predefine;
        }

        public List<String> getExcludedActions() {
            return excludedActions;
        }

        public void setExcludedActions(List<String> excludedActions) {
            this.excludedActions = excludedActions;
        }

        public PolicyStatement toStatement() {
            Role self = this;
            if (!excludedActions.isEmpty()) {
                RBACDescriptionHelper.FlattenResult fr = RBACDescriptionHelper.flatten(new HashSet<>(excludedActions), allowedActions);
                self.allowedActions = fr.normal;
            }

            PolicyStatement p = new PolicyStatement();
            p.setName(self.getName());
            p.setActions(new ArrayList<>(self.allowedActions));
            p.setEffect(self.getEffect());
            return p;
        }

        public List<PolicyStatement> toStatements() {
            return Arrays.asList(toStatement());
        }
    }

    public static class Permission {
        private Set<String> adminOnlyAPIs = new HashSet<>();
        private Set<String> normalAPIs = new HashSet<>();
        private List<Class> targetResources = new ArrayList<>();
        private Set<String> _adminOnlyAPIs = new HashSet<>();
        private Set<String> _normalAPIs = new HashSet<>();
        private String mergeTo;
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

    public static class ExpendedFieldPermissionBuilder {
        ExpendedFieldPermission fieldPermission = new ExpendedFieldPermission();
        Class basicApiClass;

        public ExpendedFieldPermissionBuilder basicApi(Class v) {
            basicApiClass = v;
            return this;
        }

        public ExpendedFieldPermissionBuilder fieldName(String v) {
            fieldPermission.fieldName = v;
            return this;
        }

        public ExpendedFieldPermissionBuilder expandTo(Class v) {
            fieldPermission.apiClass = v;
            return this;
        }

        public void build() {
            DebugUtils.Assert(fieldPermission.fieldName != null, "fieldName in ExpendedFieldPermission can not be null");
            DebugUtils.Assert(fieldPermission.apiClass != null, "apiClass in ExpendedFieldPermission can not be null");

            expendApiClassForPermissionCheck.putIfAbsent(basicApiClass, new ArrayList<>());
            expendApiClassForPermissionCheck.get(basicApiClass).add(fieldPermission);
        }
    }

    public static class PermissionBuilder {
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
                permission.get_normalAPIs().add(clz.getName());
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

        public PermissionBuilder mergeTo(String targetPermissionName) {
            permission.mergeTo = targetPermissionName;
            return this;
        }

        public Permission build() {
            permission = RBACDescriptionHelper.flatten(permission);
            DebugUtils.Assert(permissions.stream().noneMatch(it -> it.name != null && it.name.equals(permission.name)),
                    String.format("RBAC already has a permission named: %s", permission.name));

            permissions.add(permission);
            return permission;
        }
    }

    public static class RoleContributor {
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

    public static class RoleContributorBuilder {
        private RoleContributor contributor = new RoleContributor();

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

    public static class GlobalReadableResourceBuilder {
        private GlobalReadableResource readableResource = new GlobalReadableResource();

        public GlobalReadableResourceBuilder resources(Class...clzs) {
            for (Class clz : clzs) {
                readableResource.getResources().add(clz);
            }

            return this;
        }

        public void build() {
            readableResources.add(readableResource);
        }
    }

    public static class GlobalReadableResource {
        private List<Class> resources = new ArrayList<>();

        public List<Class> getResources() {
            return resources;
        }

        public void setResources(List<Class> resources) {
            this.resources = resources;
        }
    }

    private static Permission findPermissionByName(String name) {
        Optional<Permission> opt = permissions.stream().filter(p-> p.name != null && p.name.equals(name)).findFirst();
        if (!opt.isPresent()) {
            throw new CloudRuntimeException(String.format("cannot find permission[name:%s]", name));
        }

        if (opt.get().mergeTo != null) {
            return findPermissionByName(opt.get().mergeTo);
        }

        return opt.get();
    }

    private static Role findRoleByName(String name) {
        Optional<Role> opt = roles.stream().filter(r->r.name.equals(name)).findFirst();
        if (!opt.isPresent()) {
            throw new CloudRuntimeException(String.format("cannot find role[name:%s]", name));
        }
        return opt.get();
    }

    @StaticInit
    public static void staticInit() {
        BeanUtils.reflections.getSubTypesOf(RBACDescription.class).forEach(dclz-> {
            RBACDescription rd;
            try {
                rd = dclz.getConstructor().newInstance();
            } catch (Exception e) {
                throw new CloudRuntimeException(e);
            }

            rd.permissions();
            rd.roles();
            rd.contributeToRoles();
            rd.globalReadableResources();
            RBACEntityFormatter formatter =  rd.entityFormatter();
            if (formatter != null) {
                for (Class aClass : formatter.getAPIClasses()) {
                    List<Class> clzs = new ArrayList<>();
                    clzs.add(aClass);
                    clzs.addAll(BeanUtils.reflections.getSubTypesOf(aClass));
                    clzs.forEach(apiClz-> {
                        List<RBACEntityFormatter> formatters = entityFormatters.computeIfAbsent(apiClz, x->new ArrayList<>());
                        formatters.add(formatter);
                    });
                }
            }
        });

        // merge permissions
        permissions.stream().sorted(Comparator.comparing(p -> p.mergeTo != null))
                .forEach(p -> {
                    if (p.mergeTo == null) {
                        return;
                    }

                    Permission target = findPermissionByName(p.mergeTo);
                    target.getAdminOnlyAPIs().addAll(p.getAdminOnlyAPIs());
                    target.getAdminOnlyAPIs().removeAll(p.getNormalAPIs());
                    target.getNormalAPIs().addAll(p.getNormalAPIs());
                    target.getTargetResources().addAll(p.getTargetResources());
                });

        roleBuilders.forEach(rb -> {
            rb.permissionsByNames.forEach(pname -> {
                Permission permission = findPermissionByName(pname);
                rb.role.allowedActions.addAll(permission.getNormalAPIs());
            });

            roles.add(rb.role);
        });

        roleContributors.forEach(rc -> {
            Role role = findRoleByName(rc.roleName);
            rc.normalActionsByPermissionName.forEach(pname -> {
                Permission permission = findPermissionByName(pname);
                role.allowedActions.addAll(permission.getNormalAPIs());
            });
            role.allowedActions.addAll(rc.actions);
        });
    }

    static class ExpendedFieldPermission {
        String fieldName;
        Class apiClass;
    }

    public static boolean isResourceGlobalReadable(Class clz) {
        return readableResources.stream().anyMatch(r->r.resources.contains(clz));
    }

    public static boolean checkAPIPermission(APIMessage msg, boolean policyDecision) {
        List<APIPermissionCheckerWrapper> checkers = permissionCheckers.get(msg.getClass());
        if (checkers == null || checkers.isEmpty()) {
            return policyDecision;
        }

        for (APIPermissionCheckerWrapper checker : checkers) {
            Boolean ret = checker.checker.check(msg);
            if (ret == null) {
                continue;
            }

            if (checker.takeOver) {
                return ret;
            }

            if (!ret) {
                return false;
            }
        }

        return policyDecision;
    }

    public static RBACEntity formatRBACEntity(RBACEntity entity) {
        Class apiClass = entity.getApiMessage().getClass();
        List<RBACEntityFormatter> formatters = entityFormatters.get(apiClass);
        if (formatters == null) {
            return entity;
        }

        RBACEntity e;
        for (RBACEntityFormatter formatter : formatters) {
            e = formatter.format(entity);
            if (e != null) {
                return e;
            }
        }

        return entity;
    }

    public static boolean isAdminOnlyAPI(String apiName) {
        return permissions.stream().anyMatch(permission -> permission.getAdminOnlyAPIs().stream().anyMatch(api -> matcher.match(api, apiName)));
    }
}
