package org.zstack.header.securitymachine;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.securitymachine.secretresourcepool.SecretResourcePoolInventory;
import org.zstack.header.zone.ZoneInventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by LiangHanYu on 2021/11/3 15:53
 */
@Inventory(mappingVOClass = SecurityMachineVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "zone", inventoryClass = ZoneInventory.class,
                foreignKey = "zoneUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "secretResourcePool", inventoryClass = SecretResourcePoolInventory.class,
                foreignKey = "secretResourcePoolUuid", expandedInventoryKey = "uuid"),
})
public class SecurityMachineInventory implements Serializable {
    private String uuid;

    private String zoneUuid;

    private String name;

    private String secretResourcePoolUuid;

    private String description;

    private String managementIp;

    private String type;

    private String model;

    private String state;

    private String status;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    protected SecurityMachineInventory(SecurityMachineVO vo) {
        this.setUuid(vo.getUuid());
        this.setZoneUuid(vo.getZoneUuid());
        this.setName(vo.getName());
        this.setSecretResourcePoolUuid(vo.getSecretResourcePoolUuid());
        this.setDescription(vo.getDescription());
        this.setManagementIp(vo.getManagementIp());
        this.setStatus(vo.getStatus().toString());
        this.setType(vo.getType().toString());
        this.setModel(vo.getModel());
        this.setState(vo.getState().toString());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public SecurityMachineInventory() {
    }

    public static SecurityMachineInventory valueOf(SecurityMachineVO vo) {
        SecurityMachineInventory inv = new SecurityMachineInventory();
        inv.setUuid(vo.getUuid());
        inv.setZoneUuid(vo.getZoneUuid());
        inv.setName(vo.getName());
        inv.setSecretResourcePoolUuid(vo.getSecretResourcePoolUuid());
        inv.setDescription(vo.getDescription());
        inv.setManagementIp(vo.getManagementIp());
        inv.setStatus(vo.getStatus().toString());
        inv.setType(vo.getType().toString());
        inv.setModel(vo.getModel());
        inv.setState(vo.getState().toString());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<SecurityMachineInventory> valueOf(Collection<SecurityMachineVO> vos) {
        List<SecurityMachineInventory> invs = new ArrayList<SecurityMachineInventory>(vos.size());
        for (SecurityMachineVO vo : vos) {
            invs.add(SecurityMachineInventory.valueOf(vo));
        }
        return invs;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSecretResourcePoolUuid() {
        return secretResourcePoolUuid;
    }

    public void setSecretResourcePoolUuid(String secretResourcePoolUuid) {
        this.secretResourcePoolUuid = secretResourcePoolUuid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getManagementIp() {
        return managementIp;
    }

    public void setManagementIp(String managementIp) {
        this.managementIp = managementIp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
