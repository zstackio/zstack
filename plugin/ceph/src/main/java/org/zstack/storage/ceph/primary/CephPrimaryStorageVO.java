package org.zstack.storage.ceph.primary;

import org.zstack.header.storage.primary.PrimaryStorageEO;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.NoView;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by frank on 7/28/2015.
 */
@Entity
@Table
@PrimaryKeyJoinColumn(name="uuid", referencedColumnName="uuid")
@EO(EOClazz = PrimaryStorageEO.class, needView = false)
@AutoDeleteTag
public class CephPrimaryStorageVO extends PrimaryStorageVO {
    @OneToMany(fetch= FetchType.EAGER)
    @JoinColumn(name="primaryStorageUuid", insertable=false, updatable=false)
    @NoView
    private Set<CephPrimaryStorageMonVO> mons = new HashSet<CephPrimaryStorageMonVO>();
    @Column
    private String fsid;

    @Column
    private String rootVolumePoolName;
    @Column
    private String dataVolumePoolName;
    @Column
    private String imageCachePoolName;
    @Column
    private String userKey;

    public String getUserKey() {
        return userKey;
    }

    public void setUserKey(String userKey) {
        this.userKey = userKey;
    }

    public String getRootVolumePoolName() {
        return rootVolumePoolName;
    }

    public void setRootVolumePoolName(String rootVolumePoolName) {
        this.rootVolumePoolName = rootVolumePoolName;
    }

    public String getDataVolumePoolName() {
        return dataVolumePoolName;
    }

    public void setDataVolumePoolName(String dataVolumePoolName) {
        this.dataVolumePoolName = dataVolumePoolName;
    }

    public String getImageCachePoolName() {
        return imageCachePoolName;
    }

    public void setImageCachePoolName(String imageCachePoolName) {
        this.imageCachePoolName = imageCachePoolName;
    }

    public CephPrimaryStorageVO() {
    }

    public CephPrimaryStorageVO(PrimaryStorageVO other) {
        super(other);
    }

    public CephPrimaryStorageVO(CephPrimaryStorageVO other) {
        super(other);
        this.mons = other.mons;
        this.fsid = other.fsid;
    }

    public Set<CephPrimaryStorageMonVO> getMons() {
        return mons;
    }

    public void setMons(Set<CephPrimaryStorageMonVO> mons) {
        this.mons = mons;
    }

    public String getFsid() {
        return fsid;
    }

    public void setFsid(String fsid) {
        this.fsid = fsid;
    }
}
