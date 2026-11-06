package org.zstack.plugin.netprobe;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(NetProbeTaskVO.class)
public class NetProbeTaskVO_ {
    public static volatile SingularAttribute<NetProbeTaskVO, String> uuid;
    public static volatile SingularAttribute<NetProbeTaskVO, String> hostUuid;
    public static volatile SingularAttribute<NetProbeTaskVO, Timestamp> nextRunAt;
}
