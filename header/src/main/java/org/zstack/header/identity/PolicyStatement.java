package org.zstack.header.identity;

import org.zstack.header.rest.SDK;

import java.util.ArrayList;
import java.util.List;

@SDK(sdkClassName = "PolicyStatement")
public class PolicyStatement {
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
