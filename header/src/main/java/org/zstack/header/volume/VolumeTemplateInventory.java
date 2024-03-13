package org.zstack.header.volume;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@PythonClassInventory
@Inventory(mappingVOClass = VolumeTemplateVO.class)
public class VolumeTemplateInventory implements Serializable {
    private String uuid;
    private String volumeUuid;
    private String originalType;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public VolumeTemplateInventory() {
    }

    public VolumeTemplateInventory(VolumeTemplateInventory other) {
        this.uuid = other.getUuid();
        this.volumeUuid = other.getVolumeUuid();
        this.originalType = other.getOriginalType();
        this.createDate = other.getCreateDate();
        this.lastOpDate = other.getLastOpDate();
    }

    public static VolumeTemplateInventory valueOf(VolumeTemplateVO vo) {
        VolumeTemplateInventory inventory = new VolumeTemplateInventory();
        inventory.setUuid(vo.getUuid());
        inventory.setVolumeUuid(vo.getVolumeUuid());
        inventory.setOriginalType(vo.getOriginalType().toString());
        inventory.setCreateDate(vo.getCreateDate());
        inventory.setLastOpDate(vo.getLastOpDate());
        return inventory;
    }

    public static List<VolumeTemplateInventory> valueOf(Collection<VolumeTemplateVO> vos) {
        return vos.stream().map(VolumeTemplateInventory::valueOf).collect(Collectors.toList());
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getOriginalType() {
        return originalType;
    }

    public void setOriginalType(String originalType) {
        this.originalType = originalType;
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
