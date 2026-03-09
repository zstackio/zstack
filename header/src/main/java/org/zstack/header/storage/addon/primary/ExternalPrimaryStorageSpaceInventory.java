package org.zstack.header.storage.addon.primary;

import org.zstack.header.search.Inventory;

import jakarta.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = ExternalPrimaryStorageSpaceVO.class)
public class ExternalPrimaryStorageSpaceInventory implements Serializable {
    private String uuid;
    private String primaryStorageUuid;
    private String locationUrl;
    private String type;
    private String name;
    private Long availableCapacity;
    private Long totalCapacity;
    private Long availablePhysicalCapacity;
    private Long totalPhysicalCapacity;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static ExternalPrimaryStorageSpaceInventory valueOf(ExternalPrimaryStorageSpaceVO vo) {
        ExternalPrimaryStorageSpaceInventory inv = new ExternalPrimaryStorageSpaceInventory();
        inv.setUuid(vo.getUuid());
        inv.setPrimaryStorageUuid(vo.getPrimaryStorageUuid());
        inv.setLocationUrl(vo.getLocationUrl());
        inv.setType(vo.getType());
        inv.setName(vo.getName());
        inv.setAvailableCapacity(vo.getAvailableCapacity());
        inv.setTotalCapacity(vo.getTotalCapacity());
        inv.setAvailablePhysicalCapacity(vo.getAvailablePhysicalCapacity());
        inv.setTotalPhysicalCapacity(vo.getTotalPhysicalCapacity());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<ExternalPrimaryStorageSpaceInventory> valueOf(Collection<ExternalPrimaryStorageSpaceVO> vos) {
        return vos.stream().map(ExternalPrimaryStorageSpaceInventory::valueOf).collect(Collectors.toList());
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getLocationUrl() {
        return locationUrl;
    }

    public void setLocationUrl(String locationUrl) {
        this.locationUrl = locationUrl;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getAvailableCapacity() {
        return availableCapacity;
    }

    public void setAvailableCapacity(Long availableCapacity) {
        this.availableCapacity = availableCapacity;
    }

    public Long getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(Long totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public Long getAvailablePhysicalCapacity() {
        return availablePhysicalCapacity;
    }

    public void setAvailablePhysicalCapacity(Long availablePhysicalCapacity) {
        this.availablePhysicalCapacity = availablePhysicalCapacity;
    }

    public Long getTotalPhysicalCapacity() {
        return totalPhysicalCapacity;
    }

    public void setTotalPhysicalCapacity(Long totalPhysicalCapacity) {
        this.totalPhysicalCapacity = totalPhysicalCapacity;
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