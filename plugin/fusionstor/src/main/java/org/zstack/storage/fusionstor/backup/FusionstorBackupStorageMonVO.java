package org.zstack.storage.fusionstor.backup;

import org.zstack.header.storage.backup.BackupStorageEO;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.SoftDeletionCascade;
import org.zstack.header.vo.SoftDeletionCascades;
import org.zstack.storage.fusionstor.FusionstorMonAO;

import javax.persistence.*;

/**
 * Created by frank on 7/27/2015.
 */
@Entity
@Table
@Inheritance(strategy= InheritanceType.JOINED)
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = BackupStorageVO.class, joinColumn = "backupStorageUuid")
})
public class FusionstorBackupStorageMonVO extends FusionstorMonAO {
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
