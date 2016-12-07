package org.zstack.sdk;

public class LoadBalancerListenerInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
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

    public java.lang.String loadBalancerUuid;
    public void setLoadBalancerUuid(java.lang.String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }
    public java.lang.String getLoadBalancerUuid() {
        return this.loadBalancerUuid;
    }

    public java.lang.Integer instancePort;
    public void setInstancePort(java.lang.Integer instancePort) {
        this.instancePort = instancePort;
    }
    public java.lang.Integer getInstancePort() {
        return this.instancePort;
    }

    public java.lang.Integer loadBalancerPort;
    public void setLoadBalancerPort(java.lang.Integer loadBalancerPort) {
        this.loadBalancerPort = loadBalancerPort;
    }
    public java.lang.Integer getLoadBalancerPort() {
        return this.loadBalancerPort;
    }

    public java.lang.String protocol;
    public void setProtocol(java.lang.String protocol) {
        this.protocol = protocol;
    }
    public java.lang.String getProtocol() {
        return this.protocol;
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

    public java.util.List<LoadBalancerListenerVmNicRefInventory> vmNicRefs;
    public void setVmNicRefs(java.util.List<LoadBalancerListenerVmNicRefInventory> vmNicRefs) {
        this.vmNicRefs = vmNicRefs;
    }
    public java.util.List<LoadBalancerListenerVmNicRefInventory> getVmNicRefs() {
        return this.vmNicRefs;
    }

}
