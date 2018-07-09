package org.zstack.header.identity.rbac

import org.zstack.header.core.StaticInit
import org.zstack.header.exception.CloudRuntimeException
import org.zstack.header.message.APIMessage
import org.zstack.utils.BeanUtils

class RBACGroovy {
    static List<RBACInfo> rbacInfos = []
    static List<RoleInfo> roleInfos = []
    static Map<Class, List<APIPermissionChecker>> apiPermissionCheckers = [:]
    private static PolicyMatcher matcher = new PolicyMatcher()
    private static List<ContributeToRole> contributeToRoles = []

    static class ContributeToRole {
        private List<String> normalActionsReferredRBACInfos = []
        private List<String> actions = []
        String roleName

        void normalActionsFromRBAC(String...names) {
            normalActionsReferredRBACInfos.addAll(names as List)
        }

        void actions(String...ass) {
            actions.addAll(ass as List)
        }
    }

    private static RBACInfo findRBACInfoByName(String name) {
        RBACInfo info = rbacInfos.find { it.name == name }
        assert info : "cannot find RBACInfo with name[${name}]"
        return info
    }

    private static RoleInfo findRoleInfoByName(String name) {
        RoleInfo info = roleInfos.find { it.name == name }
        assert info : "cannot find RoleInfo with name[${name}]"
        return info
    }

    @StaticInit(order = -9999)
    static void staticInit() {
        roleInfos.each {role ->
            role.normalActionsReferredRBACInfoNames.each { rbacName ->
                RBACInfo ri = findRBACInfoByName(rbacName)
                role.allowedActions.addAll(ri.normalAPIs)
            }
        }

        contributeToRoles.each { ct ->
            RoleInfo role = findRoleInfoByName(ct.roleName)
            ct.normalActionsReferredRBACInfos.each { rbacName ->
                RBACInfo ri = findRBACInfoByName(rbacName)
                role.allowedActions.addAll(ri.normalAPIs)
            }
            role.allowedActions.addAll(ct.actions)
        }
    }

    static boolean checkAPIPermission(APIMessage msg, boolean policyDecision) {
        List<APIPermissionChecker> checkers = apiPermissionCheckers[msg.class]
        if (checkers == null || checkers.isEmpty()) {
            return policyDecision
        }

        for (APIPermissionChecker checker : checkers) {
            APIPermissionCheckerWrapper w = checker as APIPermissionCheckerWrapper
            Boolean ret = checker.check(msg)
            if (ret == null) {
                continue
            }

            if (w.takeOver) {
                return ret
            }

            if (!ret) {
                return false
            }
        }

        return policyDecision
    }

    static class APIPermissionCheckerWrapper implements APIPermissionChecker {
        APIPermissionChecker checker
        boolean takeOver

        @Override
        Boolean check(APIMessage msg) {
            return checker.check(msg)
        }
    }

    static void registerAPIPermissionChecker(Class<? extends APIMessage> clz, boolean takeOver,  APIPermissionChecker checker) {
        List all = [clz]
        all.addAll(BeanUtils.reflections.getSubTypesOf(clz))
        all.each {Class<? extends APIMessage>  apiClz ->
            def lst = apiPermissionCheckers.computeIfAbsent(apiClz, { return [] })
            APIPermissionCheckerWrapper wrapper = new APIPermissionCheckerWrapper(
                    takeOver: takeOver,
                    checker:  checker
            )

            lst.add(wrapper)
        }
    }

    static void registerAPIPermissionChecker(Class<? extends APIMessage> clz, APIPermissionChecker checker) {
        registerAPIPermissionChecker(clz, false, checker)
    }

    static class PermissionCheckerWrapper {
        static void check(Class<? extends APIMessage> apiClz, Closure<Boolean> closure) {
            if (apiClz != null) {
                RBACGroovy.registerAPIPermissionChecker(apiClz, [check: closure] as APIPermissionChecker)
            } else {
                def checker = [check: closure] as APIPermissionChecker
                APIMessage.apiMessageClasses.each {
                    RBACGroovy.registerAPIPermissionChecker(it, checker)
                }
            }
        }
    }

    static void permissionCheckers(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = PermissionCheckerWrapper.class) Closure closure) {
        closure.delegate = new PermissionCheckerWrapper()
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure()
    }

    static void checkIfAPIsMissingRBACInfo() {
        Set<String> allRules = []
        rbacInfos.each {
            allRules.addAll(it.adminOnlyAPIs)
            allRules.addAll(it.normalAPIs)
        }

        Set<String> missing = []
        APIMessage.apiMessageClasses.each { clz ->
            if (!allRules.any { matcher.match(it, clz.name) }) {
                missing.add(clz.name)
            }
        }

        if (!missing.isEmpty()) {
            throw new CloudRuntimeException("APIs${missing} have no rbac-info.groovy cover")
        }
    }

    static boolean isAdminOnlyAPI(String apiName) {
        return rbacInfos.any { info -> info.adminOnlyAPIs.any { matcher.match(it, apiName) } }
    }

    private static RBACInfo _flatten(RBACInfo info) {
        FlattenResult ret = flatten(info._adminOnlyAPIs, info._normalAPIs)
        info.adminOnlyAPIs = ret.adminOnly
        info.normalAPIs = ret.normal

        return info
    }

    static void permissions(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = RBACInfo.class) Closure c) {
        def info = new RBACInfo()
        c.delegate = info
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()

        info = _flatten(info)
        //rbacInfos.each { interFlatten(info, it) }
        rbacInfos.add(info)
    }

    static void rbac(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = RBACGroovy.class) Closure c) {
        c.delegate = RBACGroovy.class
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
    }

    static void role(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = RoleInfo.class) Closure c) {
        RoleInfo info = new RoleInfo()
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.delegate = info
        c()

        assert info.uuid != null : "uuid field must be set"
        roleInfos.add(info)
    }

    static class FlattenResult {
        Set<String> adminOnly = []
        Set<String> normal = []
    }

    private static void interFlatten(RBACInfo info1, RBACInfo info2) {
        FlattenResult ret = flatten(info1.adminOnlyAPIs, info2.normalAPIs)
        info1.adminOnlyAPIs = ret.adminOnly
        info2.normalAPIs = ret.normal
        ret = flatten(info2.adminOnlyAPIs, info1.normalAPIs)
        info1.normalAPIs = ret.normal
        info2.adminOnlyAPIs = ret.adminOnly
    }

    static FlattenResult flatten(Set<String> adminInput, Set<String> normalInput) {
        boolean is = adminInput.any { a -> normalInput.any { n-> matcher.match(a, n) || matcher.match(n, a) } }

        FlattenResult ret = new FlattenResult()

        if (is) {
            // declarations of admin APIs and normal APIs have conflict, flatten them to precise declarations
            APIMessage.apiMessageClasses.each { apiClz ->
                Set<String> adminRules = adminInput.findAll { matcher.match(it, apiClz.name) }
                Set<String> normalRules = normalInput.findAll { matcher.match(it, apiClz.name) }

                if (adminRules.isEmpty() && normalRules.isEmpty()) {
                    return
                }

                String winner = null

                def getWinnerRule = { String r1, String r2 ->
                    def rule = matcher.returnPrecisePattern(apiClz.name, r1, r2)
                    if (rule == null) {
                        throw new CloudRuntimeException("""ambiguous rules: ${r1} and ${r2} both matches the API[${apiClz.name}]""")
                    }
                    return rule
                }

                (adminRules + normalRules).each {
                    winner = winner == null ? it : getWinnerRule(it, winner)
                }

                assert winner != null : "${adminRules}, ${normalRules}"

                if (adminRules.contains(winner)) {
                    ret.adminOnly.add(apiClz.name)
                } else {
                    ret.normal.add(apiClz.name)
                }
            }
        } else {
            ret.adminOnly.addAll(adminInput)
            ret.normal.addAll(normalInput)
        }

        return ret
    }

    static void contributeToRole(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = ContributeToRole.class) Closure c) {
        ContributeToRole ct = new ContributeToRole()
        c.delegate = ct
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()

        assert ct.roleName != null : "roleName must be set"
        contributeToRoles.add(ct)
    }
}
