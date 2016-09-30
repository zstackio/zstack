package org.zstack.network.service.lb;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.ExpandedQueryAlias;
import org.zstack.header.query.ExpandedQueryAliases;
import org.zstack.header.search.Inventory;
import org.zstack.network.service.vip.VipInventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by frank on 8/8/2015.
 */
@Inventory(mappingVOClass = LoadBalancerVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "listeners", inventoryClass = LoadBalancerListenerInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "loadBalancerUuid"),
        @ExpandedQuery(expandedField = "vip", inventoryClass = VipInventory.class,
                foreignKey = "vipUuid", expandedInventoryKey = "uuid"),
})
public class LoadBalancerInventory {
    private String name;
    private String uuid;
    private String description;
    private String state;
    private String vipUuid;
    private List<LoadBalancerListenerInventory> listeners;

    public static LoadBalancerInventory valueOf(LoadBalancerVO vo) {
        LoadBalancerInventory inv = new LoadBalancerInventory();
        inv.setUuid(vo.getUuid());
        inv.setName(vo.getName());
        inv.setDescription(vo.getDescription());
        inv.setState(vo.getState().toString());
        inv.setVipUuid(vo.getVipUuid());
        inv.setListeners(LoadBalancerListenerInventory.valueOf(vo.getListeners()));
        return inv;
    }

    public static List<LoadBalancerInventory> valueOf(Collection<LoadBalancerVO> vos) {
        List<LoadBalancerInventory> invs = new ArrayList<LoadBalancerInventory>();
        for (LoadBalancerVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
    }

    public List<LoadBalancerListenerInventory> getListeners() {
        return listeners;
    }

    public void setListeners(List<LoadBalancerListenerInventory> listeners) {
        this.listeners = listeners;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
