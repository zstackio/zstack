package org.zstack.identity.rbac

import org.zstack.header.identity.PolicyInventory
import org.zstack.header.identity.PolicyStatement
import org.zstack.header.rest.NoSDK

class InternalPolicyDefiner {
    private List<PolicyInventory> polices = []

    @NoSDK
    static class Statement extends PolicyStatement {
        void principal(String p) {
            if (principals == null) {
                principals = []
            }

            principals.add(p)
        }

        void action(String a) {
            if (actions == null) {
                actions = []
            }

            actions.add(a)
        }

        void resource(String s) {
            if (resources == null) {
                resources = []
            }

            resources.add(s)
        }
    }

    @NoSDK
    static class Policy extends PolicyInventory {
        void statement(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = InternalPolicyDefiner.Statement.class) Closure c) {
            def s = new InternalPolicyDefiner.Statement()
            c.delegate = s
            c.resolveStrategy = Closure.DELEGATE_FIRST
            c()

            if (statements == null) {
                statements = []
            }

            statements.add(s)
        }
    }

    void policy(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = Policy.class) Closure c) {
        def p = new Policy()
        c.delegate = p
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()

        polices.add(p)
    }

    static List<PolicyInventory> New(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = InternalPolicyDefiner.class) Closure c) {
        def definer = new InternalPolicyDefiner()
        c.delegate = definer
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()

        return definer.polices
    }
}
