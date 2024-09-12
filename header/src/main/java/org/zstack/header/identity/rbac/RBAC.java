package org.zstack.header.identity.rbac;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.PackageAPIInfo;
import org.zstack.header.core.StaticInit;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.identity.SuppressCredentialCheck;
import org.zstack.header.identity.role.RolePolicyEffect;
import org.zstack.header.identity.role.RolePolicyStatement;
import org.zstack.header.identity.role.RolePolicyVO;
import org.zstack.header.message.APIMessage;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.data.Pair;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public class RBAC {
    public static List<Permission> permissions = new ArrayList<>();
    public static List<Role> roles = new ArrayList<>();
    public static Map<String, ApiPermissionBucket> apiBuckets = new HashMap<>();
    public static List<Class<?>> readableResources = new ArrayList<>();
    public static List<ResourceEnsembleMember> ensembleMembers = new ArrayList<>();

    public static Map<Class<?>, List<Function<?, List<APIMessage>>>> expendApiClassForPermissionCheck = new HashMap<>();

    private static List<RoleContributor> roleContributors = new ArrayList<>();
    private static List<RoleBuilder> roleBuilders = new ArrayList<>();

    private static PolicyMatcher matcher = new PolicyMatcher();

    public static void checkMissingRBACInfo() {
        PolicyMatcher matcher = new PolicyMatcher();

        List<String> missingInPermission = new ArrayList<>();
        List<String> missingInRole = new ArrayList<>();
        List<String> invalidPermissionNames = new ArrayList<>();
        List<String> invalidRoleNames = new ArrayList<>();

        for (Permission permission : permissions) {
            if (!permission.name.matches("[a-z0-9\\-]*")) {
                invalidPermissionNames.add(permission.name);
            }
        }

        for (Role role : roles) {
            if (!role.name.matches("[a-z0-9\\-]*")) {
                invalidRoleNames.add(role.name);
            }
        }

        APIMessage.apiMessageClasses.forEach(clz -> {
            if (clz.isAnnotationPresent(Deprecated.class) || clz.isAnnotationPresent(SuppressCredentialCheck.class)) {
                return;
            }

            String clzName = clz.getName();
            boolean has = permissions.parallelStream()
                    .anyMatch(p -> p.normalPolicies.stream().anyMatch(s -> matcher.match(s, clzName)) || p.adminOnlyPolicies.stream().anyMatch(s -> matcher.match(s, clzName)));

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
        Collections.sort(invalidPermissionNames);
        Collections.sort(invalidRoleNames);
        if (missingInPermission.isEmpty() && missingInRole.isEmpty()
                && invalidPermissionNames.isEmpty() && invalidRoleNames.isEmpty()) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        if (!missingInPermission.isEmpty()) {
            sb.append(String.format("Below APIs:\n %s not referred in any RBACInfo's permission\n", StringUtils.join(missingInPermission, "\n")));
        }

        if (!missingInRole.isEmpty()) {
            sb.append(String.format("Below APIs:\n %s not referred in any RBACInfo's role\n", StringUtils.join(missingInRole, "\n")));
        }

        if (!invalidPermissionNames.isEmpty()) {
            sb.append(String.format("Below Permission Names:\n %s are invalid. permission names must be lower case and connect by '-'\n",
                    StringUtils.join(invalidPermissionNames, "\n")));
        }

        if (!invalidRoleNames.isEmpty()) {
            sb.append(String.format("Below Role Names:\n %s are invalid. role names must be lower case and connect by '-'\n",
                    StringUtils.join(invalidRoleNames, "\n")));
        }

        throw new CloudRuntimeException(sb.toString());
    }

    public static class RoleBuilder {
        private Role role = new Role();
        private List<String> permissionsByNames = new ArrayList<>();
        private String basePermission;

        public RoleBuilder(RBACDescription description) {
            basePermission = description.permissionName();
            role.setPredefine(true);
            role.setName(basePermission);
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

        public RoleBuilder permissionBaseOnThis() {
            return permissionsByName(this.basePermission);
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

        public List<RolePolicyVO> toStatements() {
            List<RolePolicyVO> results = new ArrayList<>(allowedActions.size() + excludedActions.size());

            for (String action : allowedActions) {
                RolePolicyVO policy = new RolePolicyVO();
                policy.setActions(RolePolicyStatement.parseAction(action));
                policy.setEffect(RolePolicyEffect.Allow);
                policy.setRoleUuid(uuid);
                results.add(policy);
            }

            for (String action : excludedActions) {
                RolePolicyVO policy = new RolePolicyVO();
                policy.setActions(RolePolicyStatement.parseAction(action));
                policy.setEffect(RolePolicyEffect.Exclude);
                policy.setRoleUuid(uuid);
                results.add(policy);
            }

            return results;
        }
    }

    public static class Permission {
        private Set<Class> adminOnlyAPIs = new HashSet<>();
        private Set<Class> normalAPIs = new HashSet<>();
        private Set<String> adminOnlyPolicies = new HashSet<>();
        private Set<String> normalPolicies = new HashSet<>();
        private List<Class> targetResources = new ArrayList<>();
        private String name;
        private String basePackage;
        private List<String> requirementList = new ArrayList<>();
        private List<String> productList = new ArrayList<>();

        public Set<Class> getAdminOnlyAPIs() {
            return adminOnlyAPIs;
        }

        public Set<Class> getNormalAPIs() {
            return normalAPIs;
        }

        public Set<String> getAdminOnlyPolicies() {
            return adminOnlyPolicies;
        }

        public Set<String> getNormalPolicies() {
            return normalPolicies;
        }

        public List<Class> getTargetResources() {
            return targetResources;
        }

        public void setTargetResources(List<Class> targetResources) {
            this.targetResources = targetResources;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getBasePackage() {
            return basePackage;
        }

        public void setBasePackage(String basePackage) {
            this.basePackage = basePackage;
        }

        public List<String> getRequirementList() {
            return requirementList;
        }

        public List<String> getProductList() {
            return productList;
        }
    }

    public static class ExpendedPermission<MSG extends APIMessage> {
        public final Class<MSG> basicApiClass;

        public ExpendedPermission(Class<MSG> basicApiClass) {
            this.basicApiClass = Objects.requireNonNull(basicApiClass);
        }

        List<Function<MSG, List<APIMessage>>> expands = new ArrayList<>();

        public ExpendedPermission<MSG> expandTo(Function<MSG, List<APIMessage>> function) {
            expands.add(function);
            return this;
        }

        public void build() {
            expendApiClassForPermissionCheck.putIfAbsent(basicApiClass, new ArrayList<>());
            expendApiClassForPermissionCheck.get(basicApiClass).addAll(expands);
        }
    }

    public static class PermissionBuilder {
        Permission permission = new Permission();
        Package currentPackage;
        boolean defaultAdminOnly = false;

        private List<Class> normalAPIList = new ArrayList<>();
        private List<Class> adminOnlyAPIList = new ArrayList<>();
        private List<String> normalAPITexts = new ArrayList<>();
        private List<String> adminOnlyAPITexts = new ArrayList<>();

        public PermissionBuilder(RBACDescription description) {
            currentPackage = description.getClass().getPackage();
            permission.setName(description.permissionName());
            permission.setBasePackage(this.currentPackage.getName());
        }

        public PermissionBuilder normalAPIs(String...vs) {
            Collections.addAll(normalAPITexts, vs);
            return this;
        }

        public PermissionBuilder adminOnlyAPIs(String...vs) {
            Collections.addAll(adminOnlyAPITexts, vs);
            return this;
        }

        public PermissionBuilder adminOnlyForAll() {
            defaultAdminOnly = true;
            return this;
        }

        public PermissionBuilder normalAPIs(Class...clzs) {
            Collections.addAll(normalAPIList, clzs);
            return this;
        }

        public PermissionBuilder adminOnlyAPIs(Class...clzs) {
            Collections.addAll(adminOnlyAPIList, clzs);
            return this;
        }

        @Deprecated
        public PermissionBuilder targetResources(Class...clzs) {
            for (Class clz : clzs) {
                permission.getTargetResources().add(clz);
            }

            return this;
        }

        public PermissionBuilder communityAvailable() {
            permission.getRequirementList().add(PackageAPIInfo.PERMISSION_COMMUNITY_AVAILABLE);
            return this;
        }

        public PermissionBuilder zsvBasicAvailable() {
            permission.getRequirementList().add(PackageAPIInfo.PERMISSION_ZSV_BASIC_AVAILABLE);
            return this;
        }

        public PermissionBuilder zsvProAvailable() {
            permission.getRequirementList().add(PackageAPIInfo.PERMISSION_ZSV_PRO_AVAILABLE);
            return this;
        }

        public PermissionBuilder zsvAdvancedAvailable() {
            permission.getRequirementList().add(PackageAPIInfo.PERMISSION_ZSV_ADVANCED_AVAILABLE);
            return this;
        }

        public PermissionBuilder productName(String product) {
            permission.getProductList().add(product);
            return this;
        }

        public Permission build() {
            String packagePermission = permission.getBasePackage() + ".**";
            normalAPITexts.remove(packagePermission);
            adminOnlyAPITexts.remove(packagePermission);

            if (defaultAdminOnly) {
                adminOnlyAPITexts.add(packagePermission);
            } else {
                normalAPITexts.add(packagePermission);
            }

            permission.getNormalPolicies().addAll(normalAPITexts);
            permission.getAdminOnlyPolicies().addAll(adminOnlyAPITexts);
            permission.getNormalAPIs().addAll(normalAPIList);
            permission.getAdminOnlyAPIs().addAll(adminOnlyAPIList);

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
        private String basePermission;

        public RoleContributorBuilder(RBACDescription description) {
            this.basePermission = description.permissionName();
        }

        public RoleContributorBuilder actionsByPermissionName(String v) {
            contributor.normalActionsByPermissionName.add(v);
            return this;
        }

        public RoleContributorBuilder actionsInThisPermission() {
            return actionsByPermissionName(basePermission);
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

        /**
         * @see org.zstack.header.identity.AccountConstant#OTHER_ROLE_UUID
         */
        public RoleContributorBuilder toOtherRole() {
            return roleName("other");
        }

        public RoleContributor build() {
            roleContributors.add(contributor);
            return contributor;
        }
    }

    public static class GlobalReadableResourceBuilder {
        private List<Class<?>> readableResource = new ArrayList<>();

        public GlobalReadableResourceBuilder resources(Class<?>...clzs) {
            Collections.addAll(readableResource, clzs);
            return this;
        }

        public void build() {
            readableResources.addAll(this.readableResource);
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

    public static class ResourceEnsembleContributorBuilder {
        private final List<ResourceEnsembleMember> members = new ArrayList<>();
        private Class<?> master;

        public ResourceEnsembleContributorBuilder resource(Class<?> c) {
            ResourceEnsembleMember member = new ResourceEnsembleMember();
            member.clazz = c;
            members.add(member);
            return this;
        }

        public ResourceEnsembleContributorBuilder resourceWithCustomizeFindingMethods(
                Class<?> c,
                @Nullable Consumer<Map<String, List<String>>> findChildrenByParentUuid,
                @Nullable Consumer<Map<String, String>> findParentByChildUuid) {
            ResourceEnsembleMember member = new ResourceEnsembleMember();
            member.clazz = c;
            member.findChildrenByParentUuid = findChildrenByParentUuid;
            member.findParentByChildUuid = findParentByChildUuid;
            members.add(member);
            return this;
        }

        public ResourceEnsembleContributorBuilder contributeTo(Class<?> c) {
            master = c;
            return this;
        }

        public void build() {
            Objects.requireNonNull(master);

            ResourceEnsembleMember masterMember = findMemberFromGlobal(master);
            if (masterMember == null) {
                masterMember = new ResourceEnsembleMember();
                masterMember.setClazz(master);
                ensembleMembers.add(masterMember);
            }

            for (ResourceEnsembleMember member : members) {
                ResourceEnsembleMember existsMember = findMemberFromGlobal(member.clazz);
                if (existsMember == null) {
                    member.parent = masterMember;
                    masterMember.children.add(member);
                    ensembleMembers.add(member);
                    continue;
                }

                existsMember.parent = masterMember;
                masterMember.children.add(existsMember);
                if (existsMember.findChildrenByParentUuid == null && member.findChildrenByParentUuid != null) {
                    existsMember.findChildrenByParentUuid = member.findChildrenByParentUuid;
                }
                if (existsMember.findParentByChildUuid == null && member.findParentByChildUuid != null) {
                    existsMember.findParentByChildUuid = member.findParentByChildUuid;
                }
            }
        }

        private ResourceEnsembleMember findMemberFromGlobal(Class<?> clazz) {
            return ensembleMembers.stream()
                    .filter(c -> Objects.equals(clazz, c.getClazz()))
                    .findFirst()
                    .orElse(null);
        }
    }

    public static class ResourceEnsembleMember {
        private Class<?> clazz;
        /**
         * see: DBGraph.EntityVertex#toSQL(String, SimpleQuery.Op, String)
         */
        private Consumer<Map<String, List<String>>> findChildrenByParentUuid;
        private Consumer<Map<String, String>> findParentByChildUuid;
        private ResourceEnsembleMember parent;
        private final List<ResourceEnsembleMember> children = new ArrayList<>();

        public Class<?> getClazz() {
            return clazz;
        }

        public void setClazz(Class<?> clazz) {
            this.clazz = clazz;
        }

        public Consumer<Map<String, List<String>>> getFindChildrenByParentUuid() {
            return findChildrenByParentUuid;
        }

        public void setFindChildrenByParentUuid(Consumer<Map<String, List<String>>> findChildrenByParentUuid) {
            this.findChildrenByParentUuid = findChildrenByParentUuid;
        }

        public Consumer<Map<String, String>> getFindParentByChildUuid() {
            return findParentByChildUuid;
        }

        public void setFindParentByChildUuid(Consumer<Map<String, String>> findParentByChildUuid) {
            this.findParentByChildUuid = findParentByChildUuid;
        }

        public ResourceEnsembleMember getParent() {
            return parent;
        }

        public void setParent(ResourceEnsembleMember parent) {
            this.parent = parent;
        }

        public List<ResourceEnsembleMember> getChildren() {
            return children;
        }
    }

    private static Permission findPermissionByName(String name) {
        Optional<Permission> opt = permissions.stream().filter(p-> p.name != null && p.name.equals(name)).findFirst();
        if (!opt.isPresent()) {
            throw new CloudRuntimeException(String.format("cannot find permission[name:%s]", name));
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
        });

        buildApiBuckets();

        roleBuilders.forEach(rb -> {
            rb.permissionsByNames.forEach(pname -> {
                Permission permission = findPermissionByName(pname);
                rb.role.allowedActions.addAll(CollectionUtils.transform(permission.getNormalAPIs(), Class::getName));
                rb.role.allowedActions.addAll(permission.getNormalPolicies());
            });

            roles.add(rb.role);
        });

        roleContributors.forEach(rc -> {
            Role role = findRoleByName(rc.roleName);
            rc.normalActionsByPermissionName.forEach(pname -> {
                Permission permission = findPermissionByName(pname);
                role.allowedActions.addAll(CollectionUtils.transform(permission.getNormalAPIs(), Class::getName));
                role.allowedActions.addAll(permission.getNormalPolicies());
            });
            role.allowedActions.addAll(rc.actions);
        });
    }

    @Deprecated
    static class ExpendedFieldPermission {
        String fieldName;
        Class apiClass;
    }

    public static boolean isResourceGlobalReadable(Class clz) {
        return readableResources.stream().anyMatch(r -> r.isAssignableFrom(clz))
                || !OwnedByAccount.class.isAssignableFrom(clz);
    }

    public static boolean isValidAPI(String apiName) {
        return apiBuckets.containsKey(apiName);
    }

    public static boolean isAdminOnlyAPI(String apiName) {
        return apiBuckets.get(apiName).adminOnly;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static List<Function<APIMessage, List<APIMessage>>> expendPermissionCheckList(Class<?> apiClass) {
        final List list = expendApiClassForPermissionCheck.get(apiClass);
        if (list == null) {
            return null;
        }
        return (List<Function<APIMessage, List<APIMessage>>>) list;
    }

    public static class ApiPermissionBucket {
        public final Permission permission;
        public final boolean adminOnly;

        public ApiPermissionBucket(Permission permission, boolean adminOnly) {
            this.permission = permission;
            this.adminOnly = adminOnly;
        }
    }

    private static void buildApiBuckets() {
        List<Pair<String, Permission>> matchingList = new ArrayList<>();

        for (Permission permission : permissions) {
            for (Class<?> apiClass : permission.getNormalAPIs()) {
                apiBuckets.put(apiClass.getName(), new ApiPermissionBucket(permission, false));
            }
            for (Class<?> apiClass : permission.getAdminOnlyAPIs()) {
                apiBuckets.put(apiClass.getName(), new ApiPermissionBucket(permission, true));
            }

            for (String normalAPI : permission.getNormalPolicies()) {
                matchingList.add(new Pair<>(normalAPI, permission));
            }
            for (String adminOnlyAPI : permission.getAdminOnlyPolicies()) {
                matchingList.add(new Pair<>(adminOnlyAPI, permission));
            }
        }
        matchingList.sort(Comparator.comparingInt(it -> -it.first().length()));

        final PolicyMatcher matcher = new PolicyMatcher();
        for (Class<?> api : APIMessage.apiMessageClasses) {
            if (apiBuckets.containsKey(api.getName())) {
                continue;
            }

            String apiName = api.getName();
            Pair<String, Permission> matched = matchingList.stream()
                    .filter(pair -> matcher.match(pair.first(), apiName))
                    .findFirst()
                    .orElseThrow(() -> new CloudRuntimeException("failed to find matched permission for API:" + apiName));
            Permission permission = matched.second();
            apiBuckets.put(apiName,
                    new ApiPermissionBucket(permission, permission.getAdminOnlyPolicies().contains(matched.first())));
        }
    }
}
