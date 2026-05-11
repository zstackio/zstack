package org.zstack.header.server;

import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.core.encrypt.EncryptColumn;
import org.zstack.header.log.NoLogging;
import org.zstack.header.zone.ZoneEO;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * PhysicalServerAO intentionally does <b>not</b> implement {@code OwnedByAccount}. Physical
 * servers are infrastructure — v1.0 is admin-only (see server PRD §1.5 Out of Scope, §4.2) and
 * the OwnedByAccount interface is reserved for tenant-owned resources. Keeping it out avoids
 * the Query API filtering resources by non-admin accountUuid and forces v1.0 ownership decisions
 * to be explicit when multi-tenant PS ownership is designed in a later release.
 */
@MappedSuperclass
public class PhysicalServerAO extends ResourceVO {
    @Column
    @ForeignKey(parentEntityClass = ZoneEO.class, onDeleteAction = ReferenceOption.RESTRICT)
    private String zoneUuid;

    @Column
    @ForeignKey(parentEntityClass = ServerPoolVO.class, onDeleteAction = ReferenceOption.RESTRICT)
    private String poolUuid;

    @Column
    private String name;

    @Column
    private String description;

    @Column
    private String managementIp;

    @Column
    private String architecture;

    @Column
    private String serialNumber;

    @Column
    private String manufacturer;

    @Column
    private String model;

    @Column
    @Enumerated(EnumType.STRING)
    private PhysicalServerState state;

    @Column
    @Enumerated(EnumType.STRING)
    private PhysicalServerPowerStatus powerStatus;

    @Column
    private String oobManagementType;

    @Column
    private String oobAddress;

    @Column
    private Integer oobPort;

    @Column
    private String oobUsername;

    @EncryptColumn
    @NoLogging
    @Column
    private String oobPassword;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
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

    public PhysicalServerState getState() {
        return state;
    }

    public void setState(PhysicalServerState state) {
        this.state = state;
    }

    public PhysicalServerPowerStatus getPowerStatus() {
        return powerStatus;
    }

    public void setPowerStatus(PhysicalServerPowerStatus powerStatus) {
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

    public String getOobPassword() {
        return oobPassword;
    }

    public void setOobPassword(String oobPassword) {
        this.oobPassword = oobPassword;
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
