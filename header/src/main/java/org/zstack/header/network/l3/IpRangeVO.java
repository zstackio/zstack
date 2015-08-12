package org.zstack.header.network.l3;

import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EO;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table
@EO(EOClazz = IpRangeEO.class)
@AutoDeleteTag
public class IpRangeVO extends IpRangeAO {
    public int size() {
        return NetworkUtils.getTotalIpInRange(getStartIp(), getEndIp());
    }
}
