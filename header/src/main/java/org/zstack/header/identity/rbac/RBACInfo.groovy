package org.zstack.header.identity.rbac

import ru.lanwen.verbalregex.VerbalExpression

class RBACInfo {
    static List<RBACInfo> infos = []

    private String prefix
    private Set<String> adminOnlyAPIs = []
    private Set<String> normalAPIs = []

    static void rbac(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = RBACInfo.class) Closure c) {
        def info = new RBACInfo()
        c.delegate = info
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        infos.add(info)
    }

    VerbalExpression.Builder regex() {
        return VerbalExpression.regex()
    }

    void prefix(String value) {
        prefix = value
    }

    void adminOnlyAPIs(String...apis) {
        adminOnlyAPIs.addAll(apis as Set)
    }

    void normalAPIs(String...apis) {
        normalAPIs.addAll(apis as Set)
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
