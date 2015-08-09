package org.zstack.network.service.lb;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by frank on 8/8/2015.
 */
@Inventory(mappingVOClass = LoadBalancerVipRefVO.class)
public class LoadBalancerVipRefInventory {
    private long id;
    private String vipUuid;
    private String loadBalancerUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static LoadBalancerVipRefInventory valueOf(LoadBalancerVipRefVO vo) {
        LoadBalancerVipRefInventory inv = new LoadBalancerVipRefInventory();
        inv.setVipUuid(vo.getVipUuid());
        inv.setLoadBalancerUuid(vo.getLoadBalancerUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setId(vo.getId());
        return inv;
    }

    public static List<LoadBalancerVipRefInventory> valueOf(Collection<LoadBalancerVipRefVO> vos) {
        List<LoadBalancerVipRefInventory> invs = new ArrayList<LoadBalancerVipRefInventory>();
        for (LoadBalancerVipRefVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
    }

    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
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
