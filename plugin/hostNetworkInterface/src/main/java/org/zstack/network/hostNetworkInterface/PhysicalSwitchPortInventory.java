package org.zstack.network.hostNetworkInterface;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Physical Switch Port Inventory
 */
@Inventory(mappingVOClass = PhysicalSwitchPortVO.class)
@PythonClassInventory
public class PhysicalSwitchPortInventory implements Serializable {
    
    private String uuid;
    private String name;
    private String description;
    private String ethTrunkName;
    private String portType;
    private String peerInterfaceUuid;
    private String switchUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static PhysicalSwitchPortInventory valueOf(PhysicalSwitchPortVO vo) {
        PhysicalSwitchPortInventory inv = new PhysicalSwitchPortInventory();
        inv.setUuid(vo.getUuid());
        inv.setName(vo.getName());
        inv.setDescription(vo.getDescription());
        inv.setEthTrunkName(vo.getEthTrunkName());
        inv.setPortType(vo.getPortType());
        inv.setPeerInterfaceUuid(vo.getPeerInterfaceUuid());
        inv.setSwitchUuid(vo.getSwitchUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<PhysicalSwitchPortInventory> valueOf(Collection<PhysicalSwitchPortVO> vos) {
        List<PhysicalSwitchPortInventory> invs = new ArrayList<>();
        for (PhysicalSwitchPortVO vo : vos) {
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

    public String getEthTrunkName() {
        return ethTrunkName;
    }

    public void setEthTrunkName(String ethTrunkName) {
        this.ethTrunkName = ethTrunkName;
    }

    public String getPortType() {
        return portType;
    }

    public void setPortType(String portType) {
        this.portType = portType;
    }

    public String getPeerInterfaceUuid() {
        return peerInterfaceUuid;
    }

    public void setPeerInterfaceUuid(String peerInterfaceUuid) {
        this.peerInterfaceUuid = peerInterfaceUuid;
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

    public String getSwitchUuid() {
        return switchUuid;
    }

    public void setSwitchUuid(String switchUuid) {
        this.switchUuid = switchUuid;
    }
}
