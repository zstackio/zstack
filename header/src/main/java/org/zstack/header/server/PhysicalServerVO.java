package org.zstack.header.server;

import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.zone.ZoneEO;

import javax.persistence.*;
import java.util.Set;

@Entity
@Table(name = "PhysicalServerVO")
@BaseResource
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = ZoneEO.class, myField = "zoneUuid", targetField = "uuid"),
                @EntityGraph.Neighbour(type = ServerPoolVO.class, myField = "poolUuid", targetField = "uuid")
        }
)
public class PhysicalServerVO extends PhysicalServerAO {
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "serverUuid", insertable = false, updatable = false)
    private Set<PhysicalServerRoleVO> roles;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uuid", insertable = false, updatable = false)
    private PhysicalServerCapacityVO capacity;

    public Set<PhysicalServerRoleVO> getRoles() {
        return roles;
    }

    public void setRoles(Set<PhysicalServerRoleVO> roles) {
        this.roles = roles;
    }

    public PhysicalServerCapacityVO getCapacity() {
        return capacity;
    }

    public void setCapacity(PhysicalServerCapacityVO capacity) {
        this.capacity = capacity;
    }
}
