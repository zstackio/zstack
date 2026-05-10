package org.zstack.header.server;

import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = PhysicalServerVO.class)
public class PhysicalServerInventory implements Serializable {
    private String uuid;
    private String zoneUuid;
    private String poolUuid;
    private String name;
    private String description;
    private String managementIp;
    private String architecture;
    private String serialNumber;
    private String manufacturer;
    private String model;
    private String state;
    private String powerStatus;
    private String oobManagementType;
    private String oobAddress;
    private Integer oobPort;
    private String oobUsername;
    private List<PhysicalServerRoleInventory> roles;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static PhysicalServerInventory valueOf(PhysicalServerVO vo) {
        PhysicalServerInventory inv = new PhysicalServerInventory();
        inv.setUuid(vo.getUuid());
        inv.setZoneUuid(vo.getZoneUuid());
        inv.setPoolUuid(vo.getPoolUuid());
        inv.setName(vo.getName());
        inv.setDescription(vo.getDescription());
        inv.setManagementIp(vo.getManagementIp());
        inv.setArchitecture(vo.getArchitecture());
        inv.setSerialNumber(vo.getSerialNumber());
        inv.setManufacturer(vo.getManufacturer());
        inv.setModel(vo.getModel());
        inv.setState(vo.getState() != null ? vo.getState().toString() : null);
        inv.setPowerStatus(vo.getPowerStatus() != null ? vo.getPowerStatus().toString() : null);
        inv.setOobManagementType(vo.getOobManagementType());
        inv.setOobAddress(vo.getOobAddress());
        inv.setOobPort(vo.getOobPort());
        inv.setOobUsername(vo.getOobUsername());
        // NOTE: oobPassword intentionally excluded from inventory
        try {
            if (vo.getRoles() != null && !vo.getRoles().isEmpty()) {
                inv.setRoles(PhysicalServerRoleInventory.valueOf(vo.getRoles()));
            }
        } catch (Exception e) {
            // LAZY collection may not be initialized outside session
        }
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<PhysicalServerInventory> valueOf(Collection<PhysicalServerVO> vos) {
        List<PhysicalServerInventory> invs = new ArrayList<>();
        for (PhysicalServerVO vo : vos) {
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

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public String getPoolUuid() {
        return poolUuid;
    }

    public void setPoolUuid(String poolUuid) {
        this.poolUuid = poolUuid;
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

    public String getManagementIp() {
        return managementIp;
    }

    public void setManagementIp(String managementIp) {
        this.managementIp = managementIp;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
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

    public String getPowerStatus() {
        return powerStatus;
    }

    public void setPowerStatus(String powerStatus) {
        this.powerStatus = powerStatus;
    }

    public String getOobManagementType() {
        return oobManagementType;
    }

    public void setOobManagementType(String oobManagementType) {
        this.oobManagementType = oobManagementType;
    }

    public String getOobAddress() {
        return oobAddress;
    }

    public void setOobAddress(String oobAddress) {
        this.oobAddress = oobAddress;
    }

    public Integer getOobPort() {
        return oobPort;
    }

    public void setOobPort(Integer oobPort) {
        this.oobPort = oobPort;
    }

    public String getOobUsername() {
        return oobUsername;
    }

    public void setOobUsername(String oobUsername) {
        this.oobUsername = oobUsername;
    }

    public List<PhysicalServerRoleInventory> getRoles() {
        return roles;
    }

    public void setRoles(List<PhysicalServerRoleInventory> roles) {
        this.roles = roles;
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
