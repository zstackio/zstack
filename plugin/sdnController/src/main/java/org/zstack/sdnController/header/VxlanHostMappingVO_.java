package org.zstack.sdnController.header;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(VxlanHostMappingVO.class)
public class VxlanHostMappingVO_  {
    public static volatile SingularAttribute<VxlanHostMappingVO, Long> id;
    public static volatile SingularAttribute<VxlanHostMappingVO, String> vxlanUuid;
    public static volatile SingularAttribute<VxlanHostMappingVO, String> hostUuid;
    public static volatile SingularAttribute<VxlanHostMappingVO, Integer> vlanId;
    public static volatile SingularAttribute<VxlanHostMappingVO, String> physicalInterface;
    public static volatile SingularAttribute<VxlanHostMappingVO, Timestamp> createDate;
    public static volatile SingularAttribute<VxlanHostMappingVO, Timestamp> lastOpDate;
}