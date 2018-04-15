package org.zstack.header.identity.rbac

import org.zstack.header.identity.PolicyStatement
import org.zstack.header.identity.StatementEffect

class RoleInfo {
    String uuid
    String name
    Set<String> allowedActions = []
    StatementEffect effect = StatementEffect.Allow
    boolean adminOnly

    static List<RoleInfo> roleInfos = []

    void allowAction(String v) {
        allowedActions.add(v)
    }

    List<String> getAllowedActions() {
        return allowedActions as List
    }

    PolicyStatement toStatement() {
        RoleInfo self = this
        return new PolicyStatement(
                name: self.name,
                effect: self.effect,
                actions: self.allowedActions
        )
    }

    List<PolicyStatement> toStatements() {
        return [toStatement()]
    }

    void adminRole(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = RoleInfo.class) Closure c) {
        RoleInfo info = new RoleInfo(adminOnly: true)
        c.delegate = info
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()

        assert info.uuid != null : "uuid field must be set"
        roleInfos.add(info)
    }

    void normalRole(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = RoleInfo.class) Closure c) {
        RoleInfo info = new RoleInfo(adminOnly: false)
        c.delegate = info
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()

        assert info.uuid != null : "uuid field must be set"
        roleInfos.add(info)
    }

    static void role(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = RoleInfo.class) Closure c) {
        RoleInfo info = new RoleInfo()
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.delegate = info
        c()
    }
}
