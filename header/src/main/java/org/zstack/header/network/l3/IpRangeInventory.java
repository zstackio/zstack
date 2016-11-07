package org.zstack.header.network.l3;

import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.network.NetworkUtils;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
/**
 * @inventory
 *
 * inventory for ip range
 *
 * @category l3network
 *
 * @example
 * {
"inventory": {
"uuid": "b1cfcdeca4024d13ac82edbe8d959720",
"l3NetworkUuid": "50e637dc68b7480291ba87cbb81d94ad",
"name": "TestIpRange",
"description": "Test",
"startIp": "10.0.0.100",
"endIp": "10.10.1.200",
"netmask": "255.0.0.0",
"gateway": "10.0.0.1",
"createDate": "May 4, 2014 4:30:23 PM",
"lastOpDate": "May 4, 2014 4:30:23 PM"
}
}
 *
 * @since 0.1.0
 */
@Inventory(mappingVOClass = IpRangeVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "l3Network", inventoryClass = L3NetworkInventory.class,
                foreignKey = "l3NetworkUuid", expandedInventoryKey = "uuid")
})
public class IpRangeInventory implements Serializable{
    /**
     * @desc ip range uuid
     */
    private String uuid;
    /**
     * @desc uuid of l3Network this ip range belongs to. See :ref:`L3NetworkInventory`
     */
    private String l3NetworkUuid;
    /**
     * @desc max length of 255 characters
     */
    private String name;
    /**
     * @desc max length of 2048 characters
     */
    private String description;
    /**
     * @desc start ip address, in IPv4
     */
    private String startIp;
    /**
     * @desc end ip address, in IPv4
     */
    private String endIp;
    /**
     * @desc netmask, in IPv4
     */
    private String netmask;
    /**
     * @desc gateway, in IPv4
     */
    private String gateway;

    private String networkCidr;
    /**
     * @desc the time this resource gets created
     */
    private Timestamp createDate;
    /**
     * @desc last time this resource gets operated
     */
    private Timestamp lastOpDate;
    
    public static IpRangeInventory valueOf(IpRangeVO vo) {
        IpRangeInventory inv = new IpRangeInventory();
        inv.setCreateDate(vo.getCreateDate());
        inv.setDescription(vo.getDescription());
        inv.setEndIp(vo.getEndIp());
        inv.setGateway(vo.getGateway());
        inv.setL3NetworkUuid(vo.getL3NetworkUuid());
        inv.setName(vo.getName());
        inv.setNetmask(vo.getNetmask());
        inv.setStartIp(vo.getStartIp());
        inv.setUuid(vo.getUuid());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setNetworkCidr(vo.getNetworkCidr());
        return inv;
    }
    
    public static List<IpRangeInventory> valueOf(Collection<IpRangeVO> vos) {
        List<IpRangeInventory> invs = new ArrayList<IpRangeInventory>(vos.size());
        for (IpRangeVO vo : vos) {
            invs.add(IpRangeInventory.valueOf(vo));
        }
        return invs;
    }

    public String getNetworkCidr() {
        return networkCidr;
    }

    public void setNetworkCidr(String networkCidr) {
        this.networkCidr = networkCidr;
    }

    public String getUuid() {
        return uuid;
    }
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }
    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
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
    public String getStartIp() {
        return startIp;
    }
    public void setStartIp(String startIp) {
        this.startIp = startIp;
    }
    public String getEndIp() {
        return endIp;
    }
    public void setEndIp(String endIp) {
        this.endIp = endIp;
    }
    public String getNetmask() {
        return netmask;
    }
    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }
    public String getGateway() {
        return gateway;
    }
    public void setGateway(String gateway) {
        this.gateway = gateway;
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

    @Override
    public String toString() {
    	return JSONObjectUtil.toJsonString(this);
    }

    public static IpRangeInventory fromMessage(APIAddIpRangeMsg msg) {
        IpRangeInventory ipr = new IpRangeInventory();
        ipr.setName(msg.getName());
        ipr.setDescription(msg.getDescription());
        ipr.setStartIp(msg.getStartIp());
        ipr.setEndIp(msg.getEndIp());
        ipr.setNetmask(msg.getNetmask());
        ipr.setGateway(msg.getGateway());
        ipr.setL3NetworkUuid(msg.getL3NetworkUuid());
        SubnetUtils su = new SubnetUtils(msg.getGateway(), msg.getNetmask());
        ipr.setNetworkCidr(su.getInfo().getCidrSignature());
        ipr.setUuid(msg.getResourceUuid());
        return ipr;
    }

    public static IpRangeInventory fromMessage(APIAddIpRangeByNetworkCidrMsg msg) {
        SubnetUtils utils = new SubnetUtils(msg.getNetworkCidr());
        SubnetInfo subnet = utils.getInfo();

        IpRangeInventory ipr = new IpRangeInventory();
        ipr.setNetworkCidr(msg.getNetworkCidr());
        ipr.setName(msg.getName());
        ipr.setDescription(msg.getDescription());

        String gateway = subnet.getLowAddress();
        String startIp = NetworkUtils.longToIpv4String(NetworkUtils.ipv4StringToLong(subnet.getLowAddress()) + 1);
        String endIp = subnet.getHighAddress();
        ipr.setGateway(gateway);
        ipr.setStartIp(startIp);
        ipr.setEndIp(endIp);
        ipr.setNetmask(subnet.getNetmask());
        ipr.setL3NetworkUuid(msg.getL3NetworkUuid());
        ipr.setUuid(msg.getResourceUuid());
        return ipr;
    }

    public SubnetUtils toSubnetUtils() {
        return new SubnetUtils(new SubnetUtils(gateway, netmask).getInfo().getNetworkAddress(), netmask);
    }
}
