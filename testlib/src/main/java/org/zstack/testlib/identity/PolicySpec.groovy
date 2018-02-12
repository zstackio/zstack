package org.zstack.testlib.identity

import org.zstack.header.identity.AccountConstant
import org.zstack.sdk.PolicyInventory
import org.zstack.sdk.PolicyStatement
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.HasSession
import org.zstack.testlib.Spec
import org.zstack.testlib.SpecID
import org.zstack.testlib.SpecParam

class PolicySpec extends Spec implements HasSession {
    @SpecParam(required = true)
    String name
    @SpecParam
    String description

    private List<PolicyStatement> statements = []

    static class Statement {
        private String name
        private AccountConstant.StatementEffect effect
        private List<String> principals = []
        private List<String> actions = []
        private List<String> resources = []

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
            delegate.statements = statements.collect {
                return new org.zstack.header.identity.PolicyInventory.Statement(
                        name:it.name,
                        effect: it.effect,
                        principals: it.principals,
                        actions: it.actions,
                        resources: it.resources
                )
            }
        }

        return new SpecID(name:name, uuid:inventory.uuid)
    }

    void statement(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = Statement.class) Closure c) {
        Statement s = new Statement()
        c.delegate = s
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()

        statements.add(s)
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
