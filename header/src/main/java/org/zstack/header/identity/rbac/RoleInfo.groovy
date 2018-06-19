package org.zstack.header.identity.rbac

import org.zstack.header.identity.PolicyStatement
import org.zstack.header.identity.StatementEffect

class RoleInfo {
    String uuid
    String name
    Set<String> allowedActions = []
    StatementEffect effect = StatementEffect.Allow
    boolean adminOnly = false
    boolean predefine = true
    private List<String> normalActionsReferredRBACInfoNames = []
    List<String> excludedActions =[]

    PolicyStatement toStatement() {
        RoleInfo self = this
        if (!excludedActions.isEmpty()) {
            RBACGroovy.FlattenResult fr = RBACGroovy.flatten(excludedActions as Set, allowedActions as Set)
            self.allowedActions = fr.normal as List
        }

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
