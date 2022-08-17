package org.zstack.sdnController.header;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(VxlanMappingVO.class)
public class VxlanMappingVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<VxlanMappingVO, Integer> vni;
    public static volatile SingularAttribute<VxlanMappingVO, String> hostUuid;
    public static volatile SingularAttribute<VxlanMappingVO, Integer> vlanId;
    public static volatile SingularAttribute<VxlanMappingVO, String> physicalInterface;
    public static volatile SingularAttribute<PortLldpInfoVO, Timestamp> createDate;
    public static volatile SingularAttribute<PortLldpInfoVO, Timestamp> lastOpDate;
}