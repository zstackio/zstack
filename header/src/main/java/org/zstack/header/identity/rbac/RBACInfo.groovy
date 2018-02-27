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

    static void rbac(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = RBACInfo.class) Closure c) {
        def info = new RBACInfo()
        c.delegate = info
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        infos.add(info.flatten())
    }

    private RBACInfo flatten() {
        boolean is = false

        is = _adminOnlyAPIs.any { a -> _normalAPIs.any { n-> matcher.match(a, n)} } ||
                _normalAPIs.any { n -> _adminOnlyAPIs.any { a-> matcher.match(n, a)} }

        if (is) {
            // declarations of admin APIs and normal APIs have conflict, flatten them to precise declarations
            APIMessage.apiMessageClasses.each { apiClz ->
                Set<String> adminRules = _adminOnlyAPIs.findAll { matcher.match(it, apiClz.name) }
                Set<String> normalRules = _normalAPIs.findAll { matcher.match(it, apiClz.name) }

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
                    adminOnlyAPIs.add(winner)
                } else {
                    normalAPIs.add(winner)
                }
            }
        } else {
            adminOnlyAPIs.addAll(_adminOnlyAPIs)
            normalAPIs.addAll(_normalAPIs)
        }

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
