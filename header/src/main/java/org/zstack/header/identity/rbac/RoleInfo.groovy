package org.zstack.header.identity.rbac

import org.zstack.header.identity.PolicyInventory
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

    PolicyInventory.Statement toStatement() {
        RoleInfo self = this
        return new PolicyInventory.Statement(
                name: self.name,
                effect: self.effect,
                actions: self.allowedActions
        )
    }

    List<PolicyInventory.Statement> toStatements() {
        return [toStatement()]
    }
}
