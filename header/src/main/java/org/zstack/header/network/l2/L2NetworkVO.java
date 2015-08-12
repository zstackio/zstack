package org.zstack.header.network.l2;

import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.NoView;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table
@EO(EOClazz = L2NetworkEO.class)
@Inheritance(strategy=InheritanceType.JOINED)
@AutoDeleteTag
public class L2NetworkVO extends L2NetworkAO {
    @OneToMany(fetch=FetchType.EAGER)
    @JoinColumn(name="l2NetworkUuid", insertable=false, updatable=false)
    @NoView
    private Set<L2NetworkClusterRefVO> attachedClusterRefs = new HashSet<L2NetworkClusterRefVO>();

    public L2NetworkVO() {
    }

    public L2NetworkVO(L2NetworkVO vo) {
        this.setUuid(vo.getUuid());
        this.setAttachedClusterRefs(vo.getAttachedClusterRefs());
        this.setCreateDate(vo.getCreateDate());
        this.setDescription(vo.getDescription());
        this.setLastOpDate(vo.getLastOpDate());
        this.setName(vo.getName());
        this.setPhysicalInterface(vo.getPhysicalInterface());
        this.setType(vo.getType());
        this.setZoneUuid(vo.getZoneUuid());
    }

    public Set<L2NetworkClusterRefVO> getAttachedClusterRefs() {
        return attachedClusterRefs;
    }

    public void setAttachedClusterRefs(Set<L2NetworkClusterRefVO> attachedClusterRefs) {
        this.attachedClusterRefs = attachedClusterRefs;
    }
}
