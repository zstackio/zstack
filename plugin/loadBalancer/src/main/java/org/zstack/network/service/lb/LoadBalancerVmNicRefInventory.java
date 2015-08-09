package org.zstack.network.service.lb;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by frank on 8/8/2015.
 */
@Inventory(mappingVOClass = LoadBalancerVmNicRefVO.class)
public class LoadBalancerVmNicRefInventory {
    private long id;
    private String loadBalancerUuid;
    private String vmNicUuid;
    private String status;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static LoadBalancerVmNicRefInventory valueOf(LoadBalancerVmNicRefVO vo) {
        LoadBalancerVmNicRefInventory inv = new LoadBalancerVmNicRefInventory();
        inv.setId(vo.getId());
        inv.setLoadBalancerUuid(vo.getLoadBalancerUuid());
        inv.setVmNicUuid(vo.getVmNicUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setStatus(vo.getStatus().toString());
        return inv;
    }

    public static List<LoadBalancerVmNicRefInventory> valueOf(Collection<LoadBalancerVmNicRefVO> vos) {
        List<LoadBalancerVmNicRefInventory> invs = new ArrayList<LoadBalancerVmNicRefInventory>();
        for (LoadBalancerVmNicRefVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
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
