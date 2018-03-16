package org.zstack.header.cluster;

import org.zstack.header.hierarchy.EntityHierarchy;
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EO;
import org.zstack.header.zone.ZoneVO;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;

@Entity
@Table
@EO(EOClazz = ClusterEO.class)
@BaseResource
@EntityHierarchy(
        parent = ZoneVO.class,
        myField = "zoneUuid",
        targetField = "uuid",
        friends = {
                @EntityHierarchy.Friend(type = PrimaryStorageClusterRefVO.class, myField = "uuid", targetField = "clusterUuid")
        }
)
public class ClusterVO extends ClusterAO {
}
