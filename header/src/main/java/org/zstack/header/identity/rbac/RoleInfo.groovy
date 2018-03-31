package org.zstack.header.identity.rbac

import org.zstack.header.identity.PolicyInventory
import org.zstack.header.identity.StatementEffect

class RoleInfo {
    String uuid
    String name
    List<String> statements = []
    StatementEffect effect = StatementEffect.Allow
    boolean adminOnly

    void statement(String v) {
        statements.add(v)
    }

    PolicyInventory.Statement toStatement() {
        RoleInfo self = this
        return new PolicyInventory.Statement(
                name: self.name,
                effect: self.effect,
                actions: self.statements
        )
    }
}
