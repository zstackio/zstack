package org.zstack.header.network.l2;

import org.zstack.header.vo.*;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.zone.ZoneVO;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table
@EO(EOClazz = L2NetworkEO.class)
@BaseResource
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = ZoneVO.class, myField = "zoneUuid", targetField = "uuid")
        },
        friends = {
                @EntityGraph.Neighbour(type = L2NetworkClusterRefVO.class, myField = "uuid", targetField = "l2NetworkUuid")
        }
)
public class L2NetworkVO extends L2NetworkAO implements ToInventory {
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "l2NetworkUuid", insertable = false, updatable = false)
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
