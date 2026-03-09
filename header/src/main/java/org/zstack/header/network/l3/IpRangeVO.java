package org.zstack.header.network.l3;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EO;
import org.zstack.header.vo.EntityGraph;
import org.zstack.utils.network.NetworkUtils;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table
@EO(EOClazz = IpRangeEO.class)
@BaseResource
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = L3NetworkVO.class, myField = "l3NetworkUuid", targetField = "uuid")
        }
)
public class IpRangeVO extends IpRangeAO implements OwnedByAccount {
    @Transient
    private String accountUuid;

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }
}
