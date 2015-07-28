package org.zstack.storage.ceph.backup;

import org.zstack.header.storage.backup.BackupStorageEO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.storage.ceph.CephMonAO;

import javax.persistence.*;

/**
 * Created by frank on 7/27/2015.
 */
@Entity
@Table
@Inheritance(strategy= InheritanceType.JOINED)
public class CephBackupStorageMonVO extends CephMonAO {
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
