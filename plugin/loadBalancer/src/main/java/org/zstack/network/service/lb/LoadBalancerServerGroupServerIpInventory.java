package org.zstack.network.service.lb;

import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = LoadBalancerServerGroupServerIpVO.class)
public class LoadBalancerServerGroupServerIpInventory implements Serializable {
    private long id;
    private String description;
    private String serverGroupUuid;
    private String ipAddress;
    private String status;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static LoadBalancerServerGroupServerIpInventory valueOf(LoadBalancerServerGroupServerIpVO vo) {
        LoadBalancerServerGroupServerIpInventory inv = new LoadBalancerServerGroupServerIpInventory();
        inv.setId(vo.getId());
        inv.setDescription(vo.getDescription());
        inv.setServerGroupUuid(vo.getLoadBalancerServerGroupUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setIpAddress(vo.getIpAddress());
        inv.setStatus(vo.getStatus().toString());
        return inv;
    }

    public static List<LoadBalancerServerGroupServerIpInventory> valueOf(Collection<LoadBalancerServerGroupServerIpVO> vos) {
        List<LoadBalancerServerGroupServerIpInventory> invs = new ArrayList<LoadBalancerServerGroupServerIpInventory>();
        for (LoadBalancerServerGroupServerIpVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getServerGroupUuid() {
        return serverGroupUuid;
    }

    public void setServerGroupUuid(String serverGroupUuid) {
        this.serverGroupUuid = serverGroupUuid;
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

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
