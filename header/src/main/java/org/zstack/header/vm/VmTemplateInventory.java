package org.zstack.header.vm;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = VmTemplateVO.class, collectionValueOfMethod = "valueOf1")
public class VmTemplateInventory implements Serializable {
    private String uuid;
    private String name;
    private String instanceOfferingUuid;
    private String imageUuid;
    private String defaultL3NetworkUuid;
    private String l3NetworkUuids;
    private String type;
    private String zoneUuid;
    private String clusterUuid;
    private String hostUuid;
    private String rootDiskOfferingUuid;
    private String dataDiskOfferingUuids;
    private Integer cpuNum;
    private Long memorySize;
    private String primaryStorageUuidForRootVolume;
    private String primaryStorageUuidForDataVolume;
    private String rootVolumeSystemTags;
    private String dataVolumeSystemTags;
    private String description;
    private String strategy;
    private String systemTags;
    private String tagPatternUuids;

    protected VmTemplateInventory(VmTemplateVO vo) {
        this.setUuid(vo.getUuid());
        this.setName(vo.getName());
        this.setInstanceOfferingUuid(vo.getInstanceOfferingUuid());
        this.setImageUuid(vo.getImageUuid());
        this.setDefaultL3NetworkUuid(vo.getDefaultL3NetworkUuid());
        this.setL3NetworkUuids(vo.getL3NetworkUuids());
        this.setType(vo.getType());
        this.setZoneUuid(vo.getZoneUuid());
        this.setClusterUuid(vo.getClusterUuid());
        this.setHostUuid(vo.getHostUuid());
        this.setRootDiskOfferingUuid(vo.getRootDiskOfferingUuid());
        this.setDataDiskOfferingUuids(vo.getDataDiskOfferingUuids());
        this.setCpuNum(vo.getCpuNum());
        this.setMemorySize(vo.getMemorySize());
        this.setPrimaryStorageUuidForRootVolume(vo.getPrimaryStorageUuidForRootVolume());
        this.setPrimaryStorageUuidForDataVolume(vo.getPrimaryStorageUuidForDataVolume());
        this.setRootVolumeSystemTags(vo.getRootVolumeSystemTags());
        this.setDataVolumeSystemTags(vo.getDataVolumeSystemTags());
        this.setDescription(vo.getDescription());
        this.setStrategy(vo.getStrategy());
        this.setSystemTags(vo.getSystemTags());
        this.setTagPatternUuids(vo.getTagPatternUuids());
    }

    public static VmTemplateInventory valueOf(VmTemplateVO vo) {
        return new VmTemplateInventory(vo);
    }

    public static List<VmTemplateInventory> valueOf1(Collection<VmTemplateVO> vos) {
        List<VmTemplateInventory> invs = new ArrayList<VmTemplateInventory>(vos.size());
        for (VmTemplateVO vo : vos) {
            invs.add(VmTemplateInventory.valueOf(vo));
        }
        return invs;
    }

    public VmTemplateInventory() {
    }

    public String getTagPatternUuids() {
        return tagPatternUuids;
    }

    public void setTagPatternUuids(String tagPatternUuids) {
        this.tagPatternUuids = tagPatternUuids;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String $paramName) {
        uuid = $paramName;
    }

    public String getName() {
        return name;
    }

    public void setName(String $paramName) {
        name = $paramName;
    }

    public String getInstanceOfferingUuid() {
        return instanceOfferingUuid;
    }

    public void setInstanceOfferingUuid(String $paramName) {
        instanceOfferingUuid = $paramName;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String $paramName) {
        imageUuid = $paramName;
    }

    public String getDefaultL3NetworkUuid() {
        return defaultL3NetworkUuid;
    }

    public void setDefaultL3NetworkUuid(String $paramName) {
        defaultL3NetworkUuid = $paramName;
    }

    public String getL3NetworkUuids() {
        return l3NetworkUuids;
    }

    public void setL3NetworkUuids(String $paramName) {
        l3NetworkUuids = $paramName;
    }

    public String getType() {
        return type;
    }

    public void setType(String $paramName) {
        type = $paramName;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String $paramName) {
        zoneUuid = $paramName;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String $paramName) {
        clusterUuid = $paramName;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String $paramName) {
        hostUuid = $paramName;
    }

    public String getRootDiskOfferingUuid() {
        return rootDiskOfferingUuid;
    }

    public void setRootDiskOfferingUuid(String $paramName) {
        rootDiskOfferingUuid = $paramName;
    }

    public String getDataDiskOfferingUuids() {
        return dataDiskOfferingUuids;
    }

    public void setDataDiskOfferingUuids(String $paramName) {
        dataDiskOfferingUuids = $paramName;
    }

    public Integer getCpuNum() {
        return cpuNum;
    }

    public void setCpuNum(Integer $paramName) {
        cpuNum = $paramName;
    }

    public Long getMemorySize() {
        return memorySize;
    }

    public void setMemorySize(Long $paramName) {
        memorySize = $paramName;
    }

    public String getPrimaryStorageUuidForRootVolume() {
        return primaryStorageUuidForRootVolume;
    }

    public void setPrimaryStorageUuidForRootVolume(String $paramName) {
        primaryStorageUuidForRootVolume = $paramName;
    }

    public String getPrimaryStorageUuidForDataVolume() {
        return primaryStorageUuidForDataVolume;
    }

    public void setPrimaryStorageUuidForDataVolume(String $paramName) {
        primaryStorageUuidForDataVolume = $paramName;
    }

    public String getRootVolumeSystemTags() {
        return rootVolumeSystemTags;
    }

    public void setRootVolumeSystemTags(String $paramName) {
        rootVolumeSystemTags = $paramName;
    }

    public String getDataVolumeSystemTags() {
        return dataVolumeSystemTags;
    }

    public void setDataVolumeSystemTags(String $paramName) {
        dataVolumeSystemTags = $paramName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String $paramName) {
        description = $paramName;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String $paramName) {
        strategy = $paramName;
    }

    public String getSystemTags() {
        return systemTags;
    }

    public void setSystemTags(String $paramName) {
        systemTags = $paramName;
    }
}
