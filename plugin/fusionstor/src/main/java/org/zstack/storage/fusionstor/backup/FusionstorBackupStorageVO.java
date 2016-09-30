package org.zstack.storage.fusionstor.backup;

import org.zstack.header.storage.backup.BackupStorageEO;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.NoView;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by frank on 7/27/2015.
 */
@Entity
@Table
@PrimaryKeyJoinColumn(name="uuid", referencedColumnName="uuid")
@EO(EOClazz = BackupStorageEO.class, needView = false)
@AutoDeleteTag
public class FusionstorBackupStorageVO extends BackupStorageVO {
    @OneToMany(fetch= FetchType.EAGER)
    @JoinColumn(name="backupStorageUuid", insertable=false, updatable=false)
    @NoView
    private Set<FusionstorBackupStorageMonVO> mons = new HashSet<FusionstorBackupStorageMonVO>();

    @Column
    private String poolName;

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    @Column
    private String fsid;

    public String getFsid() {
        return fsid;
    }

    public void setFsid(String fsid) {

        this.fsid = fsid;
    }

    public FusionstorBackupStorageVO() {
    }

    public FusionstorBackupStorageVO(BackupStorageVO vo) {
        super(vo);
    }

    public FusionstorBackupStorageVO(FusionstorBackupStorageVO other) {
        super(other);
        this.mons = other.mons;
    }

    public Set<FusionstorBackupStorageMonVO> getMons() {
        return mons;
    }

    public void setMons(Set<FusionstorBackupStorageMonVO> mons) {
        this.mons = mons;
    }
}
