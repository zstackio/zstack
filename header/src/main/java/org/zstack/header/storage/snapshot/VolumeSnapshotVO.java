package org.zstack.header.storage.snapshot;

import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.NoView;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 */
@Entity
@Table
@EO(EOClazz = VolumeSnapshotEO.class)
@BaseResource
public class VolumeSnapshotVO extends VolumeSnapshotAO {
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "volumeSnapshotUuid", insertable = false, updatable = false)
    @NoView
    private List<VolumeSnapshotBackupStorageRefVO> backupStorageRefs = new ArrayList<VolumeSnapshotBackupStorageRefVO>();

    public List<VolumeSnapshotBackupStorageRefVO> getBackupStorageRefs() {
        return backupStorageRefs;
    }

    public void setBackupStorageRefs(List<VolumeSnapshotBackupStorageRefVO> backupStorageRefs) {
        this.backupStorageRefs = backupStorageRefs;
    }
}
