package org.zstack.storage.ceph.primary;

import org.zstack.header.storage.primary.PrimaryStorageEO;
import org.zstack.header.storage.primary.PrimaryStorageVO;
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
public class CephPrimaryStorageVO extends PrimaryStorageVO {
    @OneToMany(fetch= FetchType.EAGER)
    @JoinColumn(name="primaryStorageUuid", insertable=false, updatable=false)
    @NoView
    private Set<CephPrimaryStorageMonVO> mons = new HashSet<CephPrimaryStorageMonVO>();

    @Column
    private String fsid;

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
