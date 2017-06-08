package org.zstack.network.service.vip;

import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
/**
 * @inventory
 * inventory for vip
 *
 * @category vip
 *
 * @example
 *{
"inventory": {
"uuid": "a6e0feb8191538f991672b6a1cb4fa17",
"name": "vip",
"ipRangeUuid": "6c620829bd8d4c948bfad3b64a12e00e",
"l3NetworkUuid": "d0aff3c3e0104b089d90e7efebd84a7c",
"ip": "192.168.1.50",
"gateway": "192.168.1.1",
"netmask": "255.255.255.0",
"createDate": "May 13, 2014 10:25:06 PM",
"lastOpDate": "May 13, 2014 10:25:06 PM"
}
}
 * @since 0.1.0
 */
@Inventory(mappingVOClass = VipVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "l3Network", inventoryClass = L3NetworkInventory.class,
                foreignKey = "l3NetworkUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "peerL3Network", inventoryClass = L3NetworkInventory.class,
                foreignKey = "peerL3NetworkUuid", expandedInventoryKey = "uuid"),
})
public class VipInventory implements Serializable {
    /**
     * @desc vip uuid
     */
    private String uuid;
    /**
     * @desc max length of 255 characters
     */
    private String name;
    /**
     * @desc max length of 2048 characters
     */
    private String description;
    /**
     * @ignore
     */
    @APINoSee
    private String ipRangeUuid;
    /**
     * @desc uuid of l3Network this vip is on
     */
    private String l3NetworkUuid;
    /**
     * @desc ip address in IPv4
     */
    private String ip;
    /**
     * @desc
     * @choices Enabled,Disabled
     */
    private String state;
    /**
     * @desc gateway in IPv4
     */
    private String gateway;
    /**
     * @desc netmask in IPv4
     */
    private String netmask;
    /**
     * @desc service provider name that provides vip service
     */
    private String serviceProvider;
    /**
     * @desc uuid of l3Network this vip used for. For example, when vip is used as Eip, the guest network is the peer network
     */
    private String peerL3NetworkUuid;
    /**
     * @desc service name this vip used for. For example, PortForwarding
     */
    private String useFor;

    @APINoSee
    private String usedIpUuid;
    /**
     * @desc the time this resource gets created
     */
    private Timestamp createDate;
    /**
     * @desc last time this resource gets operated
     */
    private Timestamp lastOpDate;

    public static VipInventory valueOf(VipVO vo) {
        VipInventory inv = new VipInventory();
        inv.setName(vo.getName());
        inv.setDescription(vo.getDescription());
        inv.setCreateDate(vo.getCreateDate());
        inv.setGateway(vo.getGateway());
        inv.setIp(vo.getIp());
        inv.setIpRangeUuid(vo.getIpRangeUuid());
        inv.setServiceProvider(vo.getServiceProvider());
        inv.setL3NetworkUuid(vo.getL3NetworkUuid());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setNetmask(vo.getNetmask());
        inv.setUseFor(vo.getUseFor());
        inv.setUuid(vo.getUuid());
        inv.setPeerL3NetworkUuid(vo.getPeerL3NetworkUuid());
        inv.setState(vo.getState().toString());
        inv.setUsedIpUuid(vo.getUsedIpUuid());
        return inv;
    }

    public static List<VipInventory> valueOf(Collection<VipVO> vos) {
        List<VipInventory> invs = new ArrayList<VipInventory>(vos.size());
        for (VipVO vo : vos) {
            invs.add(VipInventory.valueOf(vo));
        }
        return invs;
    }

    public String getUsedIpUuid() {
        return usedIpUuid;
    }

    public void setUsedIpUuid(String usedIpUuid) {
        this.usedIpUuid = usedIpUuid;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPeerL3NetworkUuid() {
        return peerL3NetworkUuid;
    }

    public void setPeerL3NetworkUuid(String peerL3NetworkUuid) {
        this.peerL3NetworkUuid = peerL3NetworkUuid;
    }

    public String getUuid() {
        return uuid;
    }
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getServiceProvider() {
        return serviceProvider;
    }

    public void setServiceProvider(String serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    public String getUseFor() {
        return useFor;
    }

    public void setUseFor(String useFor) {
        this.useFor = useFor;
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
    public String getIpRangeUuid() {
        return ipRangeUuid;
    }
    public void setIpRangeUuid(String ipRangeUuid) {
        this.ipRangeUuid = ipRangeUuid;
    }
    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }
    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }
    public String getIp() {
        return ip;
    }
    public void setIp(String ip) {
        this.ip = ip;
    }
    public String getGateway() {
        return gateway;
    }
    public void setGateway(String gateway) {
        this.gateway = gateway;
    }
    public String getNetmask() {
        return netmask;
    }
    public void setNetmask(String netmask) {
        this.netmask = netmask;
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
}
