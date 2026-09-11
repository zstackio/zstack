package org.zstack.network.service.lb;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * a flattened backend server view of a load balancer server group,
 * carrying the vm instance name and nic ip joined from VmInstanceVO/VmNicVO
 */
public class LoadBalancerServerGroupBackendServerInventory implements Serializable {
    /* deterministic uuid derived from serverType and the backend ref id, stable across requests */
    private String uuid;
    private String serverGroupUuid;
    private String serverGroupName;
    private String listenerUuid;
    private String loadBalancerUuid;
    /* VmInstance or ServerIp */
    private String serverType;
    private String vmNicUuid;
    private String vmInstanceUuid;
    private String serverName;
    private String ip;
    private String l3NetworkUuid;
    private Long weight;
    private Integer ipVersion;
    private String state;
    private String runtimeStatus;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getServerGroupName() {
        return serverGroupName;
    }

    public void setServerGroupName(String serverGroupName) {
        this.serverGroupName = serverGroupName;
    }

    public String getServerGroupUuid() {
        return serverGroupUuid;
    }

    public void setServerGroupUuid(String serverGroupUuid) {
        this.serverGroupUuid = serverGroupUuid;
    }

    public String getListenerUuid() {
        return listenerUuid;
    }

    public void setListenerUuid(String listenerUuid) {
        this.listenerUuid = listenerUuid;
    }

    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }

    public String getServerType() {
        return serverType;
    }

    public void setServerType(String serverType) {
        this.serverType = serverType;
    }

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public Long getWeight() {
        return weight;
    }

    public void setWeight(Long weight) {
        this.weight = weight;
    }

    public Integer getIpVersion() {
        return ipVersion;
    }

    public void setIpVersion(Integer ipVersion) {
        this.ipVersion = ipVersion;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getRuntimeStatus() {
        return runtimeStatus;
    }

    public void setRuntimeStatus(String runtimeStatus) {
        this.runtimeStatus = runtimeStatus;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
