package org.zstack.header.identity;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.identity.AccountConstant.StatementEffect;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.ExpandedQueryAlias;
import org.zstack.header.query.ExpandedQueryAliases;
import org.zstack.header.rest.SDK;
import org.zstack.header.search.Inventory;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = PolicyVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "account", inventoryClass = AccountInventory.class,
                foreignKey = "accountUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "groupRef", inventoryClass = UserGroupPolicyRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "policyUuid", hidden = true),
        @ExpandedQuery(expandedField = "userRef", inventoryClass = UserPolicyRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "policyUuid", hidden = true)
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(alias = "group", expandedField = "groupRef.group"),
        @ExpandedQueryAlias(alias = "user", expandedField = "userRef.user")
})
public class PolicyInventory {
    @SDK(sdkClassName = "PolicyStatement")
    public static class Statement {
        private String name;
        private StatementEffect effect;
        private List<String> principals;
        private List<String> actions;
        private List<String> resources;


        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public StatementEffect getEffect() {
            return effect;
        }

        public void setEffect(StatementEffect effect) {
            this.effect = effect;
        }

        public List<String> getPrincipals() {
            return principals;
        }

        public void setPrincipals(List<String> principals) {
            this.principals = principals;
        }

        public List<String> getActions() {
            return actions;
        }

        public void setActions(List<String> actions) {
            this.actions = actions;
        }

        public List<String> getResources() {
            return resources;
        }

        public void setResources(List<String> resources) {
            this.resources = resources;
        }

        public void addAction(String a) {
            if (actions == null) {
                actions = new ArrayList<String>();
            }
            actions.add(a);
        }
    }

    public static PolicyInventory valueOf(PolicyVO vo) {
        PolicyInventory inv = new PolicyInventory();
        inv.setName(vo.getName());
        inv.setUuid(vo.getUuid());
        inv.setStatements(JSONObjectUtil.toCollection(vo.getData(), ArrayList.class, Statement.class));
        inv.setAccountUuid(vo.getAccountUuid());
        return inv;
    }

    public static List<PolicyInventory> valueOf(Collection<PolicyVO> vos) {
        List<PolicyInventory> invs = new ArrayList<PolicyInventory>();
        for (PolicyVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    private List<Statement> statements;
    private String name;
    private String uuid;
    private String accountUuid;

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public void addStatement(Statement s) {
        if (statements == null) {
            statements = new ArrayList<Statement>();
        }

        statements.add(s);
    }

    public void addStatement(List<Statement> s) {
        if (statements == null) {
            statements = new ArrayList<Statement>();
        }

        statements.addAll(s);
    }

    public List<Statement> getStatements() {
        return statements;
    }

    public void setStatements(List<Statement> statements) {
        this.statements = statements;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
