package org.zstack.header.identity.rbac

import org.zstack.header.identity.PolicyStatement
import org.zstack.header.identity.StatementEffect

class RoleInfo {
    String uuid
    String name
    List<String> allowedActions = []
    StatementEffect effect = StatementEffect.Allow
    boolean adminOnly

    void allowAction(String v) {
        allowedActions.add(v)
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
}
