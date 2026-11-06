package org.zstack.plugin.netprobe;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(NetProbeResultVO.class)
public class NetProbeResultVO_ {
    public static volatile SingularAttribute<NetProbeResultVO, Long> id;
    public static volatile SingularAttribute<NetProbeResultVO, String> taskUuid;
}
