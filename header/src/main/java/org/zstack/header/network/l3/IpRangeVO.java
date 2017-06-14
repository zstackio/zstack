package org.zstack.header.network.l3;

import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EO;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table
@EO(EOClazz = IpRangeEO.class)
@BaseResource
public class IpRangeVO extends IpRangeAO {
    public int size() {
        return NetworkUtils.getTotalIpInRange(getStartIp(), getEndIp());
    }
}
