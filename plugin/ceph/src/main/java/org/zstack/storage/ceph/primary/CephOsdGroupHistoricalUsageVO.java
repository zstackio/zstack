package org.zstack.storage.ceph.primary;

import org.zstack.header.storage.primary.HistoricalUsageAO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;

import javax.persistence.*;

@Entity
@Table
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = CephOsdGroupVO.class, myField = "osdGroupUuid", targetField = "uuid"),
        }
)
public class CephOsdGroupHistoricalUsageVO extends HistoricalUsageAO {
    @Column
    @ForeignKey(parentEntityClass = CephOsdGroupVO.class, onDeleteAction = ReferenceOption.CASCADE)
    private String osdGroupUuid;

    public CephOsdGroupHistoricalUsageVO() {

    }

    public String getOsdGroupUuid() {
        return osdGroupUuid;
    }

    public void setOsdGroupUuid(String osdGroupUuid) {
        this.osdGroupUuid = osdGroupUuid;
    }

    @Override
    public String getResourceUuid() {
        return osdGroupUuid;
    }

    @Override
    public void setResourceUuid(String resourceUuid) {
        this.osdGroupUuid = resourceUuid;
    }
}
