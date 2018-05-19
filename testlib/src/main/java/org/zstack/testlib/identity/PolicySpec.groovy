package org.zstack.testlib.identity

import org.zstack.sdk.PolicyInventory
import org.zstack.sdk.PolicyStatement
import org.zstack.sdk.PolicyStatementEffect
import org.zstack.testlib.*

class PolicySpec extends Spec implements HasSession {
    @SpecParam(required = true)
    String name
    @SpecParam
    String description

    private List<PolicyStatement> statements = []

    static class Statement {
        List<String> principals = []
        List<String> actions = []
        List<String> resources = []

        void principal(String p)  {
            principals.add(p)
        }

        void action(String a) {
            actions.add(a)
        }

        void resource(String r) {
            resources.add(r)
        }
    }

    PolicySpec(EnvSpec envSpec) {
        super(envSpec)
    }

    PolicyInventory inventory

    @Override
    SpecID create(String uuid, String sessionId) {
        inventory = createPolicy {
            delegate.resourceUuid = uuid
            delegate.name = name
            delegate.description = description
            delegate.sessionId = sessionId
            delegate.statements = statements
        }

        if (parent instanceof RoleSpec) {
            postCreate {
                attachPolicyToRole {
                    roleUuid = (parent as RoleSpec).inventory.uuid
                    policyUuid = inventory.uuid
                }
            }
        }

        return id(name, inventory.uuid)
    }

    void allow(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value=Statement.class) Closure c) {
        Statement s = new Statement()
        c.delegate = s
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()

        statements.add(new PolicyStatement(
                name: "allow-statement",
                effect: PolicyStatementEffect.Allow,
                actions: s.actions,
                principals: s.principals,
                resources: s.resources
        ))
    }

    void deny(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value=Statement.class) Closure c) {
        Statement s = new Statement()
        c.delegate = s
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()

        statements.add(new PolicyStatement(
                name: "deny-statement",
                effect: PolicyStatementEffect.Deny,
                actions: s.actions,
                principals: s.principals,
                resources: s.resources
        ))
    }

    @Override
    void delete(String sessionId) {
        if (inventory != null) {
            deletePolicy {
                delegate.uuid = inventory.uuid
                delegate.sessionId = sessionId
            }
        }
    }
}
