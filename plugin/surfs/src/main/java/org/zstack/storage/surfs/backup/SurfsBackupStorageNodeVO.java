package org.zstack.storage.surfs.backup;

import org.zstack.header.storage.backup.BackupStorageEO;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.SoftDeletionCascade;
import org.zstack.header.vo.SoftDeletionCascades;
import org.zstack.storage.surfs.SurfsNodeAO;

import javax.persistence.*;

/**
 * Created by zhouhaiping 2017-08-31
 */
@Entity
@Table
@Inheritance(strategy= InheritanceType.JOINED)
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = BackupStorageVO.class, joinColumn = "backupStorageUuid")
})
public class SurfsBackupStorageNodeVO extends SurfsNodeAO {
    @Column
    @ForeignKey(parentEntityClass = BackupStorageEO.class, parentKey = "uuid", onDeleteAction = ReferenceOption.CASCADE)
    private String backupStorageUuid;

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }
}
