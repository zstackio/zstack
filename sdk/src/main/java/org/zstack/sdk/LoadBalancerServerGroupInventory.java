package org.zstack.sdk;

import java.sql.Timestamp;
import java.util.List;

public class LoadBalancerServerGroupInventory {

    public java.lang.String uuid;
    public java.lang.String  getUuid() {
        return uuid;
    }
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public java.lang.String name;
    public java.lang.String  getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public java.lang.String description;
    public java.lang.String  getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public java.lang.String loadBalancerUuid;
    public java.lang.String  getLoadBalancerUuid() {
        return loadBalancerUuid;
    }
    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }

    public java.lang.Integer weight;
    public java.lang.Integer getWeight() {
        return weight;
    }
    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public java.sql.Timestamp createDate;
    public void setCreateDate(java.sql.Timestamp createDate) {
        this.createDate = createDate;
    }
    public java.sql.Timestamp getCreateDate() {
        return this.createDate;
    }

    public java.sql.Timestamp lastOpDate;
    public void setLastOpDate(java.sql.Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
    public java.sql.Timestamp getLastOpDate() {
        return this.lastOpDate;
    }

    public java.util.List listenerServerGroupRefs;
    public java.util.List getListenerServerGroupRefs() {
        return listenerServerGroupRefs;
    }
    public void setListenerServerGroupRefs(List listenerServerGroupRefs) {
        this.listenerServerGroupRefs = listenerServerGroupRefs;
    }

    public java.util.List serverIps;
    public java.util.List getServerIps() {
        return serverIps;
    }
    public void setServerIps(List serverIps) {
        this.serverIps = serverIps;
    }

    public java.util.List vmNicRefs;
    public java.util.List getVmNicRefs() {
        return vmNicRefs;
    }
    public void setVmNicRefs(List vmNicRefs) {
        this.vmNicRefs = vmNicRefs;
    }

}
