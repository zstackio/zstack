package org.zstack.header.image;

import org.zstack.header.storage.backup.BackupStorageEO;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.SoftDeletionCascade;
import org.zstack.header.vo.SoftDeletionCascades;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

/**
 */
@Entity
@Table
@SoftDeletionCascades({
        @SoftDeletionCascade(parent = ImageVO.class, joinColumn = "imageUuid"),
        @SoftDeletionCascade(parent = BackupStorageVO.class, joinColumn = "backupStorageUuid")
})
public class ImageBackupStorageRefVO implements Serializable {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column
    @ForeignKey(parentEntityClass = ImageEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String imageUuid;
    @Column
    @ForeignKey(parentEntityClass = BackupStorageEO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String backupStorageUuid;
    @Column
    @Enumerated(EnumType.STRING)
    private ImageStatus status;
    @Column
    private String installPath;
    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;

    public ImageStatus getStatus() {
        return status;
    }

    public void setStatus(ImageStatus status) {
        this.status = status;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
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
