package org.zstack.header.host;


import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(HostNUMATopologyVO.class)
public class HostNUMATopologyVO_ {
    public static volatile SingularAttribute<HostNUMATopologyVO, Long> id;
    public static volatile SingularAttribute<HostNUMATopologyVO, String> uuid;
    public static volatile SingularAttribute<HostNUMATopologyVO, Integer> nodeID;
    public static volatile SingularAttribute<HostNUMATopologyVO, String> nodeCPUs;
    public static volatile SingularAttribute<HostNUMATopologyVO, Long> nodeMemSize;
    public static volatile SingularAttribute<HostNUMATopologyVO, String> nodeDistance;
}
