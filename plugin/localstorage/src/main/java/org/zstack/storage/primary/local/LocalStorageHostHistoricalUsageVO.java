package org.zstack.storage.primary.local;

import org.zstack.header.storage.primary.HistoricalUsageAO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;

import javax.persistence.*;

@Entity
@Table
@org.zstack.header.vo.EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = LocalStorageHostRefVO.class, myField = "hostUuid", targetField = "hostUuid")
        }
)
public class LocalStorageHostHistoricalUsageVO extends HistoricalUsageAO {
    @Column
    @ForeignKey(parentEntityClass = LocalStorageHostRefVO.class, parentKey = "hostUuid", onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String hostUuid;

    public LocalStorageHostHistoricalUsageVO() {
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    @Override
    public String getResourceUuid() {
        return hostUuid;
    }

    @Override
    public void setResourceUuid(String resourceUuid) {
        this.hostUuid = resourceUuid;
    }
}
