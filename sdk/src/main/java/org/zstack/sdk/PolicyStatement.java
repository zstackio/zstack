package org.zstack.sdk;

import org.zstack.sdk.PolicyStatementEffect;

public class PolicyStatement  {

    public java.lang.String name;
    public void setName(java.lang.String name) {
        this.name = name;
    }
    public java.lang.String getName() {
        return this.name;
    }

    public PolicyStatementEffect effect;
    public void setEffect(PolicyStatementEffect effect) {
        this.effect = effect;
    }
    public PolicyStatementEffect getEffect() {
        return this.effect;
    }

    public java.util.List principals;
    public void setPrincipals(java.util.List principals) {
        this.principals = principals;
    }
    public java.util.List getPrincipals() {
        return this.principals;
    }

    public java.util.List actions;
    public void setActions(java.util.List actions) {
        this.actions = actions;
    }
    public java.util.List getActions() {
        return this.actions;
    }

    public java.util.List resources;
    public void setResources(java.util.List resources) {
        this.resources = resources;
    }
    public java.util.List getResources() {
        return this.resources;
    }

}
