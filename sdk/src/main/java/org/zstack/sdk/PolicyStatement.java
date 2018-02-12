package org.zstack.sdk;

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

    public java.util.List<String> principals;
    public void setPrincipals(java.util.List<String> principals) {
        this.principals = principals;
    }
    public java.util.List<String> getPrincipals() {
        return this.principals;
    }

    public java.util.List<String> actions;
    public void setActions(java.util.List<String> actions) {
        this.actions = actions;
    }
    public java.util.List<String> getActions() {
        return this.actions;
    }

    public java.util.List<String> resources;
    public void setResources(java.util.List<String> resources) {
        this.resources = resources;
    }
    public java.util.List<String> getResources() {
        return this.resources;
    }

}
