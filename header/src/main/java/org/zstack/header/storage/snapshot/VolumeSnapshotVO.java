package org.zstack.header.storage.snapshot;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.NoView;
import org.zstack.header.volume.VolumeVO;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 */
@Entity
@Table
@EO(EOClazz = VolumeSnapshotEO.class)
@BaseResource
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = VolumeVO.class, myField = "volumeUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = VolumeSnapshotTreeVO.class, myField = "treeUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = PrimaryStorageVO.class, myField = "primaryStorageUuid", targetField = "uuid"),
        }
)
public class VolumeSnapshotVO extends VolumeSnapshotAO implements OwnedByAccount {
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "volumeSnapshotUuid", insertable = false, updatable = false)
    @NoView
    private List<VolumeSnapshotBackupStorageRefVO> backupStorageRefs = new ArrayList<VolumeSnapshotBackupStorageRefVO>();

    @Transient
    private String accountUuid;

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public List<VolumeSnapshotBackupStorageRefVO> getBackupStorageRefs() {
        return backupStorageRefs;
    }

    public void setBackupStorageRefs(List<VolumeSnapshotBackupStorageRefVO> backupStorageRefs) {
        this.backupStorageRefs = backupStorageRefs;
    }
}
