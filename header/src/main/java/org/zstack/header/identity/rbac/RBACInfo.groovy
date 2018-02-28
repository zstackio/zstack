package org.zstack.header.identity.rbac

import org.zstack.header.exception.CloudRuntimeException
import org.zstack.header.message.APIMessage

class RBACInfo {
    static List<RBACInfo> infos = []
    private static PolicyMatcher matcher = new PolicyMatcher()

    private String prefix
    private Set<String> adminOnlyAPIs = []
    private Set<String> normalAPIs = []

    private Set<String> _adminOnlyAPIs = []
    private Set<String> _normalAPIs = []

    static void checkIfAPIsMissingRBACInfo() {
        Set<String> allRules = []
        infos.each {
            allRules.addAll(it.adminOnlyAPIs)
            allRules.addAll(it.normalAPIs)
        }

        Set<String> missing = []
        APIMessage.apiMessageClasses.each { clz ->
            if (!allRules.any { matcher.match(it, clz.name)}) {
                missing.add(clz.name)
            }
        }

        if (!missing.isEmpty()) {
            throw new CloudRuntimeException("APIs${missing} have no rbac-info.groovy cover")
        }
    }

    static void rbac(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = RBACInfo.class) Closure c) {
        def info = new RBACInfo()
        c.delegate = info
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()

        info = info.flatten()
        infos.each { interFlatten(info, it) }
        infos.add(info)
    }

    private static class FlattenResult {
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

    private static FlattenResult flatten(Set<String> adminInput, Set<String> normalInput) {
        boolean is = adminInput.any { a -> normalInput.any { n-> matcher.match(a, n)} } ||
                normalInput.any { n -> adminInput.any { a-> matcher.match(n, a)} }

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
                    ret.adminOnly.add(winner)
                } else {
                    ret.normal.add(winner)
                }
            }
        } else {
            ret.adminOnly.addAll(adminInput)
            ret.normal.addAll(normalInput)
        }

        return ret
    }

    private RBACInfo flatten() {
        FlattenResult ret = flatten(_adminOnlyAPIs, _normalAPIs)
        adminOnlyAPIs = ret.adminOnly
        normalAPIs = ret.normal

        return this
    }

    void prefix(String value) {
        prefix = value
    }

    void adminOnlyAPIs(String...apis) {
        _adminOnlyAPIs.addAll(apis as Set)
    }

    void normalAPIs(String...apis) {
        _normalAPIs.addAll(apis as Set)
    }

    String getPrefix() {
        return prefix
    }

    Set<String> getAdminOnlyAPIs() {
        return adminOnlyAPIs
    }

    Set<String> getNormalAPIs() {
        return normalAPIs
    }
}
