package org.zstack.sdk;

public class LoadBalancerInventory  {

    public java.lang.String name;
    public void setName(java.lang.String name) {
        this.name = name;
    }
    public java.lang.String getName() {
        return this.name;
    }

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String description;
    public void setDescription(java.lang.String description) {
        this.description = description;
    }
    public java.lang.String getDescription() {
        return this.description;
    }

    public java.lang.String state;
    public void setState(java.lang.String state) {
        this.state = state;
    }
    public java.lang.String getState() {
        return this.state;
    }

    public java.lang.String vipUuid;
    public void setVipUuid(java.lang.String vipUuid) {
        this.vipUuid = vipUuid;
    }
    public java.lang.String getVipUuid() {
        return this.vipUuid;
    }

    public java.util.List<LoadBalancerListenerInventory> listeners;
    public void setListeners(java.util.List<LoadBalancerListenerInventory> listeners) {
        this.listeners = listeners;
    }
    public java.util.List<LoadBalancerListenerInventory> getListeners() {
        return this.listeners;
    }

}
