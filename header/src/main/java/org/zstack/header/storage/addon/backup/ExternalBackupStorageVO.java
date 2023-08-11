package org.zstack.header.storage.addon.backup;

import org.zstack.header.storage.backup.BackupStorageEO;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

@Entity
@Table
@PrimaryKeyJoinColumn(name="uuid", referencedColumnName="uuid")
@EO(EOClazz = BackupStorageEO.class, needView = false)
@AutoDeleteTag
public class ExternalBackupStorageVO extends BackupStorageVO {

    @Column
    private String identity;

    public ExternalBackupStorageVO() {
    }

    public ExternalBackupStorageVO(BackupStorageVO vo) {
        super(vo);
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }
}
