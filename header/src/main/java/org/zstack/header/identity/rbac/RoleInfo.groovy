package org.zstack.header.identity.rbac

import org.zstack.header.identity.PolicyStatement
import org.zstack.header.identity.StatementEffect

class RoleInfo {
    String uuid
    String name
    Set<String> allowedActions = []
    StatementEffect effect = StatementEffect.Allow
    boolean adminOnly = false
    private List<String> normalActionsReferredRBACInfoNames = []

    PolicyStatement toStatement() {
        RoleInfo self = this
        return new PolicyStatement(
                name: self.name,
                effect: self.effect,
                actions: self.allowedActions as List
        )
    }

    List<PolicyStatement> toStatements() {
        return [toStatement()]
    }

    void normalActionsFromRBAC(String...names) {
        normalActionsReferredRBACInfoNames.addAll(names as List)
    }

    void actions(String...actions) {
        allowedActions.addAll(actions as List)
    }
}
