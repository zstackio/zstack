package org.zstack.storage.ceph.backup;

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
public class CephBackupStorageVO extends BackupStorageVO {
    @OneToMany(fetch= FetchType.EAGER)
    @JoinColumn(name="backupStorageUuid", insertable=false, updatable=false)
    @NoView
    private Set<CephBackupStorageMonVO> mons = new HashSet<CephBackupStorageMonVO>();

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

    public CephBackupStorageVO() {
    }

    public CephBackupStorageVO(BackupStorageVO vo) {
        super(vo);
    }

    public CephBackupStorageVO(CephBackupStorageVO other) {
        super(other);
        this.mons = other.mons;
    }

    public Set<CephBackupStorageMonVO> getMons() {
        return mons;
    }

    public void setMons(Set<CephBackupStorageMonVO> mons) {
        this.mons = mons;
    }
}
