package org.zstack.network.service.lb;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by frank on 8/8/2015.
 */
@Inventory(mappingVOClass = LoadBalancerListenerVO.class)
public class LoadBalancerListenerInventory {
    private String uuid;
    private String name;
    private String description;
    private String loadBalancerUuid;
    private int instancePort;
    private int loadBalancerPort;
    private String protocol;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static LoadBalancerListenerInventory valueOf(LoadBalancerListenerVO vo) {
        LoadBalancerListenerInventory inv = new LoadBalancerListenerInventory();
        inv.setUuid(vo.getUuid());
        inv.setLoadBalancerUuid(vo.getLoadBalancerUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setInstancePort(vo.getInstancePort());
        inv.setLoadBalancerPort(vo.getLoadBalancerPort());
        inv.setProtocol(vo.getProtocol());
        inv.setName(vo.getName());
        inv.setDescription(vo.getDescription());
        return inv;
    }

    public static List<LoadBalancerListenerInventory> valueOf(Collection<LoadBalancerListenerVO> vos) {
        List<LoadBalancerListenerInventory> invs = new ArrayList<LoadBalancerListenerInventory>();
        for (LoadBalancerListenerVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
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

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }

    public int getInstancePort() {
        return instancePort;
    }

    public void setInstancePort(int instancePort) {
        this.instancePort = instancePort;
    }

    public int getLoadBalancerPort() {
        return loadBalancerPort;
    }

    public void setLoadBalancerPort(int loadBalancerPort) {
        this.loadBalancerPort = loadBalancerPort;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
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
