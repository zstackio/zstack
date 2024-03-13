package org.zstack.header.volume;

import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ToInventory;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
@BaseResource
public class VolumeTemplateVO extends ResourceVO implements ToInventory {
    @Column
    @ForeignKey(parentEntityClass = VolumeEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String volumeUuid;

    @Enumerated(EnumType.STRING)
    private VolumeType originalType;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public VolumeType getOriginalType() {
        return originalType;
    }

    public void setOriginalType(VolumeType originalType) {
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
