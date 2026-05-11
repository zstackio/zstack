package org.zstack.header.server;

import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = ServerPoolVO.class)
public class ServerPoolInventory implements Serializable {
    private String uuid;
    private String name;
    private String description;
    private String zoneUuid;
    private String physicalLocation;
    private String networkTopology;
    private String state;
    private boolean isDefault;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static ServerPoolInventory valueOf(ServerPoolVO vo) {
        ServerPoolInventory inv = new ServerPoolInventory();
        inv.setUuid(vo.getUuid());
        inv.setName(vo.getName());
        inv.setDescription(vo.getDescription());
        inv.setZoneUuid(vo.getZoneUuid());
        inv.setPhysicalLocation(vo.getPhysicalLocation());
        inv.setNetworkTopology(vo.getNetworkTopology());
        inv.setState(vo.getState() != null ? vo.getState().toString() : null);
        inv.setDefault(vo.isDefault());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<ServerPoolInventory> valueOf(Collection<ServerPoolVO> vos) {
        List<ServerPoolInventory> invs = new ArrayList<>();
        for (ServerPoolVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public String getPhysicalLocation() {
        return physicalLocation;
    }

    public void setPhysicalLocation(String physicalLocation) {
        this.physicalLocation = physicalLocation;
    }

    public String getNetworkTopology() {
        return networkTopology;
    }

    public void setNetworkTopology(String networkTopology) {
        this.networkTopology = networkTopology;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
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
