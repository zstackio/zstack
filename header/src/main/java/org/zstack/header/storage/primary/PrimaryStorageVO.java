package org.zstack.header.storage.primary;

import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.NoView;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table
@EO(EOClazz = PrimaryStorageEO.class)
@BaseResource
public class PrimaryStorageVO extends PrimaryStorageAO {
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "primaryStorageUuid", insertable = false, updatable = false)
    @NoView
    private Set<PrimaryStorageClusterRefVO> attachedClusterRefs = new HashSet<PrimaryStorageClusterRefVO>();

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "uuid")
    @NoView
    private PrimaryStorageCapacityVO capacity;

    public PrimaryStorageVO() {
    }

    public PrimaryStorageVO(PrimaryStorageVO other) {
        super(other);
        this.attachedClusterRefs = other.attachedClusterRefs;
        this.capacity = other.capacity;
    }

    public Set<PrimaryStorageClusterRefVO> getAttachedClusterRefs() {
        return attachedClusterRefs;
    }

    public void setAttachedClusterRefs(Set<PrimaryStorageClusterRefVO> attachedClusterRefs) {
        this.attachedClusterRefs = attachedClusterRefs;
    }

    public PrimaryStorageCapacityVO getCapacity() {
        return capacity;
    }

    public void setCapacity(PrimaryStorageCapacityVO capacity) {
        this.capacity = capacity;
    }
}
