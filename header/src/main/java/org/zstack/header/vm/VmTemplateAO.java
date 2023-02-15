package org.zstack.header.vm;

import org.zstack.header.vo.ToInventory;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

/**
 * @ Author : yh.w
 * @ Date   : Created in 9:55 2022/12/7
 */
@MappedSuperclass
public class VmTemplateAO implements ToInventory {
    @Column
    @Id
    private String uuid;

    @Column
    private String name;

    @Column
    private String instanceOfferingUuid;

    @Column
    private String imageUuid;

    @Column
    private String defaultL3NetworkUuid;

    @Column
    private String l3NetworkUuids;

    @Column
    private String type;

    @Column
    private String zoneUuid;

    @Column
    private String clusterUuid;

    @Column
    private String hostUuid;

    @Column
    private String rootDiskOfferingUuid;
    @Column
    private String dataDiskOfferingUuids;

    @Column
    private Integer cpuNum;

    @Column
    private Long memorySize;

    @Column
    private String primaryStorageUuidForRootVolume;

    @Column
    private String primaryStorageUuidForDataVolume;

    @Column
    private String rootVolumeSystemTags;

    @Column
    private String dataVolumeSystemTags;

    @Column
    private String description;

    @Column
    private String strategy;

    @Column
    private String systemTags;

    @Column
    private String tagPatternUuids;

    public VmTemplateAO() {
    }

    public VmTemplateAO(VmTemplateAO other) {
        this.uuid = other.uuid;
        this.name = other.name;
        this.instanceOfferingUuid = other.instanceOfferingUuid;
        this.imageUuid = other.imageUuid;
        this.defaultL3NetworkUuid = other.defaultL3NetworkUuid;
        this.l3NetworkUuids = other.l3NetworkUuids;
        this.type = other.type;
        this.zoneUuid = other.zoneUuid;
        this.clusterUuid = other.clusterUuid;
        this.hostUuid = other.hostUuid;
        this.rootDiskOfferingUuid = other.rootDiskOfferingUuid;
        this.dataDiskOfferingUuids = other.dataDiskOfferingUuids;
        this.cpuNum = other.cpuNum;
        this.memorySize = other.memorySize;
        this.primaryStorageUuidForRootVolume = other.primaryStorageUuidForRootVolume;
        this.primaryStorageUuidForDataVolume = other.primaryStorageUuidForDataVolume;
        this.rootVolumeSystemTags = other.rootVolumeSystemTags;
        this.dataVolumeSystemTags = other.dataVolumeSystemTags;
        this.description = other.description;
        this.strategy = other.strategy;
        this.systemTags = other.systemTags;
        this.tagPatternUuids = other.tagPatternUuids;
    }

    public String getTagPatternUuids() {
        return tagPatternUuids;
    }

    public void setTagPatternUuids(String tagPatternUuids) {
        this.tagPatternUuids = tagPatternUuids;
    }

    public String getRootVolumeSystemTags() {
        return rootVolumeSystemTags;
    }

    public void setRootVolumeSystemTags(String rootVolumeSystemTags) {
        this.rootVolumeSystemTags = rootVolumeSystemTags;
    }

    public String getDataVolumeSystemTags() {
        return dataVolumeSystemTags;
    }

    public void setDataVolumeSystemTags(String dataVolumeSystemTags) {
        this.dataVolumeSystemTags = dataVolumeSystemTags;
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

    public String getInstanceOfferingUuid() {
        return instanceOfferingUuid;
    }

    public void setInstanceOfferingUuid(String instanceOfferingUuid) {
        this.instanceOfferingUuid = instanceOfferingUuid;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public String getDefaultL3NetworkUuid() {
        return defaultL3NetworkUuid;
    }

    public void setDefaultL3NetworkUuid(String defaultL3NetworkUuid) {
        this.defaultL3NetworkUuid = defaultL3NetworkUuid;
    }

    public String getL3NetworkUuids() {
        return l3NetworkUuids;
    }

    public void setL3NetworkUuids(String l3NetworkUuids) {
        this.l3NetworkUuids = l3NetworkUuids;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getRootDiskOfferingUuid() {
        return rootDiskOfferingUuid;
    }

    public void setRootDiskOfferingUuid(String rootDiskOfferingUuid) {
        this.rootDiskOfferingUuid = rootDiskOfferingUuid;
    }

    public String getDataDiskOfferingUuids() {
        return dataDiskOfferingUuids;
    }

    public void setDataDiskOfferingUuids(String dataDiskOfferingUuids) {
        this.dataDiskOfferingUuids = dataDiskOfferingUuids;
    }

    public Integer getCpuNum() {
        return cpuNum;
    }

    public void setCpuNum(Integer cpuNum) {
        this.cpuNum = cpuNum;
    }

    public Long getMemorySize() {
        return memorySize;
    }

    public void setMemorySize(Long memorySize) {
        this.memorySize = memorySize;
    }

    public String getPrimaryStorageUuidForRootVolume() {
        return primaryStorageUuidForRootVolume;
    }

    public void setPrimaryStorageUuidForRootVolume(String primaryStorageUuidForRootVolume) {
        this.primaryStorageUuidForRootVolume = primaryStorageUuidForRootVolume;
    }

    public String getPrimaryStorageUuidForDataVolume() {
        return primaryStorageUuidForDataVolume;
    }

    public void setPrimaryStorageUuidForDataVolume(String primaryStorageUuidForDataVolume) {
        this.primaryStorageUuidForDataVolume = primaryStorageUuidForDataVolume;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public String getSystemTags() {
        return systemTags;
    }

    public void setSystemTags(String systemTags) {
        this.systemTags = systemTags;
    }
}
