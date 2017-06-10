package org.zstack.header.storage.primary;

import org.zstack.header.host.HostEO;
import org.zstack.header.host.HostVO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.SoftDeletionCascade;
import org.zstack.header.vo.SoftDeletionCascades;


import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by Administrator on 2017-05-08.
 */

@Entity
@Table
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = PrimaryStorageVO.class, joinColumn = "primaryStorageUuid"),
        @SoftDeletionCascade(parent = HostVO.class, joinColumn = "hostUuid")
})
@IdClass(CompositePrimaryKeyForPrimaryStorageHostRefVO.class)
public class PrimaryStorageHostRefVO {
    @Column
    @Id
    @ForeignKey(parentEntityClass = HostEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String hostUuid;

    @Column
    @Id
    @ForeignKey(parentEntityClass = PrimaryStorageEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String primaryStorageUuid;

    @Column
    @Enumerated(EnumType.STRING)
    private PrimaryStorageHostStatus status;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public void setStatus(PrimaryStorageHostStatus status) {
        this.status = status;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public PrimaryStorageHostStatus getStatus() {
        return status;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }
}
