package org.zstack.header.storage.primary;

import org.zstack.header.vo.EO;
import org.zstack.header.vo.NoView;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Inheritance(strategy=InheritanceType.JOINED)
@Table
@NamedQueries({ 
	@NamedQuery(name = "PrimaryStorageVO.countLeft",
	        query = "select count(p) from PrimaryStorageVO p where p.uuid != :uuid and p.uuid in (select psc.primaryStorageUuid from PrimaryStorageClusterRefVO psc where psc.clusterUuid = :clusterUuid)"),
})
@EO(EOClazz = PrimaryStorageEO.class)
public class PrimaryStorageVO extends PrimaryStorageAO {
    @OneToMany(fetch=FetchType.EAGER)
    @JoinColumn(name="primaryStorageUuid", insertable=false, updatable=false)
    @NoView
    private Set<PrimaryStorageClusterRefVO> attachedClusterRefs = new HashSet<PrimaryStorageClusterRefVO>();

    @OneToOne(fetch=FetchType.EAGER)
    @JoinColumn(name="uuid")
    @NoView
    private PrimaryStorageCapacityVO capacity;

	public PrimaryStorageVO() {
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
