package org.zstack.sdnController.header;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(VxlanClusterMappingVO.class)
public class VxlanClusterMappingVO_ {
    public static volatile SingularAttribute<VxlanClusterMappingVO, Long> id;
    public static volatile SingularAttribute<VxlanClusterMappingVO, String> vxlanUuid;
    public static volatile SingularAttribute<VxlanClusterMappingVO, String> clusterUuid;
    public static volatile SingularAttribute<VxlanClusterMappingVO, Integer> vlanId;
    public static volatile SingularAttribute<VxlanClusterMappingVO, String> physicalInterface;
    public static volatile SingularAttribute<VxlanClusterMappingVO, Timestamp> createDate;
    public static volatile SingularAttribute<VxlanClusterMappingVO, Timestamp> lastOpDate;
}