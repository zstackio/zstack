package org.zstack.sdk;



public class SecurityGroupTemplateInventory  {

    public java.lang.String id;
    public void setId(java.lang.String id) {
        this.id = id;
    }
    public java.lang.String getId() {
        return this.id;
    }

    public java.lang.String name;
    public void setName(java.lang.String name) {
        this.name = name;
    }
    public java.lang.String getName() {
        return this.name;
    }

    public java.lang.String description;
    public void setDescription(java.lang.String description) {
        this.description = description;
    }
    public java.lang.String getDescription() {
        return this.description;
    }

    public boolean defaultSelected;
    public void setDefaultSelected(boolean defaultSelected) {
        this.defaultSelected = defaultSelected;
    }
    public boolean getDefaultSelected() {
        return this.defaultSelected;
    }

    public java.util.List rules;
    public void setRules(java.util.List rules) {
        this.rules = rules;
    }
    public java.util.List getRules() {
        return this.rules;
    }

}
