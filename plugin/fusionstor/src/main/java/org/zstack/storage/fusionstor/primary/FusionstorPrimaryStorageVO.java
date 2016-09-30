package org.zstack.storage.fusionstor.primary;

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
public class FusionstorPrimaryStorageVO extends PrimaryStorageVO {
    @OneToMany(fetch= FetchType.EAGER)
    @JoinColumn(name="primaryStorageUuid", insertable=false, updatable=false)
    @NoView
    private Set<FusionstorPrimaryStorageMonVO> mons = new HashSet<FusionstorPrimaryStorageMonVO>();

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

    public FusionstorPrimaryStorageVO() {
    }

    public FusionstorPrimaryStorageVO(PrimaryStorageVO other) {
        super(other);
    }

    public FusionstorPrimaryStorageVO(FusionstorPrimaryStorageVO other) {
        super(other);
        this.mons = other.mons;
        this.fsid = other.fsid;
    }

    public Set<FusionstorPrimaryStorageMonVO> getMons() {
        return mons;
    }

    public void setMons(Set<FusionstorPrimaryStorageMonVO> mons) {
        this.mons = mons;
    }

    public String getFsid() {
        return fsid;
    }

    public void setFsid(String fsid) {
        this.fsid = fsid;
    }
}
