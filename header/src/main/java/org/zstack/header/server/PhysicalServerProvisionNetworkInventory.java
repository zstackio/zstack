package org.zstack.header.server;

import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = PhysicalServerProvisionNetworkVO.class)
public class PhysicalServerProvisionNetworkInventory implements Serializable {
    private String uuid;
    private String zoneUuid;
    private String name;
    private String description;
    private String type;
    private String dhcpInterface;
    private String dhcpRangeStartIp;
    private String dhcpRangeEndIp;
    private String dhcpRangeNetmask;
    private String dhcpRangeGateway;
    private String state;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private List<String> attachedClusterUuids;
    private List<String> attachedPoolUuids;

    public static PhysicalServerProvisionNetworkInventory valueOf(PhysicalServerProvisionNetworkVO vo) {
        PhysicalServerProvisionNetworkInventory inv = new PhysicalServerProvisionNetworkInventory();
        inv.setUuid(vo.getUuid());
        inv.setZoneUuid(vo.getZoneUuid());
        inv.setName(vo.getName());
        inv.setDescription(vo.getDescription());
        inv.setType(vo.getType() != null ? vo.getType().toString() : null);
        inv.setDhcpInterface(vo.getDhcpInterface());
        inv.setDhcpRangeStartIp(vo.getDhcpRangeStartIp());
        inv.setDhcpRangeEndIp(vo.getDhcpRangeEndIp());
        inv.setDhcpRangeNetmask(vo.getDhcpRangeNetmask());
        inv.setDhcpRangeGateway(vo.getDhcpRangeGateway());
        inv.setState(vo.getState() != null ? vo.getState().toString() : null);
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        try {
            if (vo.getClusterRefs() != null && !vo.getClusterRefs().isEmpty()) {
                List<String> clusterUuids = new ArrayList<>();
                for (PhysicalServerProvisionNetworkClusterRefVO ref : vo.getClusterRefs()) {
                    clusterUuids.add(ref.getClusterUuid());
                }
                inv.setAttachedClusterUuids(clusterUuids);
            }
        } catch (Exception e) {
            // LAZY collection may not be initialized outside session
        }
        try {
            if (vo.getPoolRefs() != null && !vo.getPoolRefs().isEmpty()) {
                List<String> poolUuids = new ArrayList<>();
                for (PhysicalServerProvisionNetworkPoolRefVO ref : vo.getPoolRefs()) {
                    poolUuids.add(ref.getPoolUuid());
                }
                inv.setAttachedPoolUuids(poolUuids);
            }
        } catch (Exception e) {
            // LAZY collection may not be initialized outside session
        }
        return inv;
    }

    public static List<PhysicalServerProvisionNetworkInventory> valueOf(Collection<PhysicalServerProvisionNetworkVO> vos) {
        List<PhysicalServerProvisionNetworkInventory> invs = new ArrayList<>();
        for (PhysicalServerProvisionNetworkVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getZoneUuid() { return zoneUuid; }
    public void setZoneUuid(String zoneUuid) { this.zoneUuid = zoneUuid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDhcpInterface() { return dhcpInterface; }
    public void setDhcpInterface(String dhcpInterface) { this.dhcpInterface = dhcpInterface; }

    public String getDhcpRangeStartIp() { return dhcpRangeStartIp; }
    public void setDhcpRangeStartIp(String dhcpRangeStartIp) { this.dhcpRangeStartIp = dhcpRangeStartIp; }

    public String getDhcpRangeEndIp() { return dhcpRangeEndIp; }
    public void setDhcpRangeEndIp(String dhcpRangeEndIp) { this.dhcpRangeEndIp = dhcpRangeEndIp; }

    public String getDhcpRangeNetmask() { return dhcpRangeNetmask; }
    public void setDhcpRangeNetmask(String dhcpRangeNetmask) { this.dhcpRangeNetmask = dhcpRangeNetmask; }

    public String getDhcpRangeGateway() { return dhcpRangeGateway; }
    public void setDhcpRangeGateway(String dhcpRangeGateway) { this.dhcpRangeGateway = dhcpRangeGateway; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public Timestamp getCreateDate() { return createDate; }
    public void setCreateDate(Timestamp createDate) { this.createDate = createDate; }

    public Timestamp getLastOpDate() { return lastOpDate; }
    public void setLastOpDate(Timestamp lastOpDate) { this.lastOpDate = lastOpDate; }

    public List<String> getAttachedClusterUuids() { return attachedClusterUuids; }
    public void setAttachedClusterUuids(List<String> attachedClusterUuids) { this.attachedClusterUuids = attachedClusterUuids; }

    public List<String> getAttachedPoolUuids() { return attachedPoolUuids; }
    public void setAttachedPoolUuids(List<String> attachedPoolUuids) { this.attachedPoolUuids = attachedPoolUuids; }
}
