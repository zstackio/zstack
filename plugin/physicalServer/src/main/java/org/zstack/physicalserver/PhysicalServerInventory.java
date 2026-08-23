package org.zstack.physicalserver;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = PhysicalServerVO.class)
@PythonClassInventory
public class PhysicalServerInventory implements Serializable {
    private String uuid;
    private String zoneUuid;
    private String serialNumber;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static PhysicalServerInventory valueOf(PhysicalServerVO vo) {
        PhysicalServerInventory inventory = new PhysicalServerInventory();
        inventory.setUuid(vo.getUuid());
        inventory.setZoneUuid(vo.getZoneUuid());
        inventory.setSerialNumber(vo.getSerialNumber());
        inventory.setCreateDate(vo.getCreateDate());
        inventory.setLastOpDate(vo.getLastOpDate());
        return inventory;
    }

    public static List<PhysicalServerInventory> valueOf(Collection<PhysicalServerVO> vos) {
        List<PhysicalServerInventory> inventories = new ArrayList<>(vos.size());
        for (PhysicalServerVO vo : vos) {
            inventories.add(valueOf(vo));
        }
        return inventories;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
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
