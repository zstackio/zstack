package org.zstack.header.identity;

import org.zstack.header.rest.SDK;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.ArrayList;
import java.util.List;

@SDK(sdkClassName = "PolicyStatement")
public class PolicyStatement {
    private String name;
    private StatementEffect effect;
    private List<String> principals;
    private List<String> actions;
    private List<String> resources;

    public String toJSON() {
        return JSONObjectUtil.toJsonString(this);
    }

    public static PolicyStatement fromJSON(String json) {
        return JSONObjectUtil.toObject(json, PolicyStatement.class);
    }

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
            actions = new ArrayList<>();
        }
        actions.add(a);
    }


    public static PolicyStatementBuilder builder() {
        return new PolicyStatementBuilder();
    }

    public static final class PolicyStatementBuilder {
        private String name;
        private StatementEffect effect;
        private List<String> principals;
        private List<String> actions;
        private List<String> resources;

        private PolicyStatementBuilder() {
        }

        public PolicyStatementBuilder name(String name) {
            this.name = name;
            return this;
        }

        public PolicyStatementBuilder effect(StatementEffect effect) {
            this.effect = effect;
            return this;
        }

        public PolicyStatementBuilder principals(List<String> principals) {
            this.principals = principals;
            return this;
        }

        public PolicyStatementBuilder actions(List<String> actions) {
            this.actions = actions;
            return this;
        }

        public PolicyStatementBuilder resources(List<String> resources) {
            this.resources = resources;
            return this;
        }

        public PolicyStatement build() {
            PolicyStatement policyStatement = new PolicyStatement();
            DebugUtils.Assert(effect != null, "effect cannot be null");
            policyStatement.setName(name);
            policyStatement.setEffect(effect);
            policyStatement.setPrincipals(principals);
            policyStatement.setActions(actions);
            policyStatement.setResources(resources);
            return policyStatement;
        }
    }
}
