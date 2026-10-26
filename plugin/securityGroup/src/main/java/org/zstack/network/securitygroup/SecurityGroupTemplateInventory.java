package org.zstack.network.securitygroup;

import org.zstack.header.configuration.PythonClassInventory;

import java.util.List;

@PythonClassInventory
public class SecurityGroupTemplateInventory {
    private String id;
    private String name;
    private String description;
    private boolean defaultSelected;
    private List<SecurityGroupTemplateRule> rules;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDefaultSelected() {
        return defaultSelected;
    }

    public void setDefaultSelected(boolean defaultSelected) {
        this.defaultSelected = defaultSelected;
    }

    public List<SecurityGroupTemplateRule> getRules() {
        return rules;
    }

    public void setRules(List<SecurityGroupTemplateRule> rules) {
        this.rules = rules;
    }
}
