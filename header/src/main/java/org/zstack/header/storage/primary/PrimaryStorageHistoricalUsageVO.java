package org.zstack.header.storage.primary;

import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;

import javax.persistence.*;

@Entity
@Table
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = PrimaryStorageCapacityVO.class, myField = "primaryStorageUuid", targetField = "uuid")
        }
)
public class PrimaryStorageHistoricalUsageVO extends HistoricalUsageAO {
    @Column
    @ForeignKey(parentEntityClass = PrimaryStorageCapacityVO.class, parentKey = "uuid", onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String primaryStorageUuid;

    public PrimaryStorageHistoricalUsageVO() {
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    @Override
    public String getResourceUuid() {
        return primaryStorageUuid;
    }

    @Override
    public void setResourceUuid(String resourceUuid) {
        this.primaryStorageUuid = resourceUuid;
    }
}
