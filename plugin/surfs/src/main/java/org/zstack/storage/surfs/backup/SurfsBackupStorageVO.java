package org.zstack.storage.surfs.backup;

import java.util.HashSet;
import java.util.Set;

import org.zstack.header.storage.backup.BackupStorageEO;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.NoView;
import org.zstack.storage.surfs.backup.SurfsBackupStorageNodeVO;

import javax.persistence.*;


/**
 * Created by zhouhaiping on 2017-07-18
 */
@Entity
@Table
@PrimaryKeyJoinColumn(name="uuid", referencedColumnName="uuid")
@EO(EOClazz = BackupStorageEO.class, needView = false)
@AutoDeleteTag
public class SurfsBackupStorageVO extends BackupStorageVO {
    @OneToMany(fetch= FetchType.EAGER)
    @JoinColumn(name="backupStorageUuid", insertable=false, updatable=false)
    @NoView
    private Set<SurfsBackupStorageNodeVO> nodes = new HashSet<SurfsBackupStorageNodeVO>();	
	
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
    
    public SurfsBackupStorageVO() {
    }

    public SurfsBackupStorageVO(BackupStorageVO vo) {
        super(vo);
    }

    public SurfsBackupStorageVO(SurfsBackupStorageVO other) {
        super(other);
        this.nodes=other.nodes;
    }

    public Set<SurfsBackupStorageNodeVO> getNodes() {
        return nodes;
    }

    public void setNodes(Set<SurfsBackupStorageNodeVO> nodes) {
        this.nodes = nodes;
    }    

}
