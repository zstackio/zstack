package org.zstack.sdnController.header;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(PortLldpInfoVO.class)
public class PortLldpInfoVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<PortLldpInfoVO, String> l2NetworkUuid;
    public static volatile SingularAttribute<PortLldpInfoVO, String> interfaceName;
    public static volatile SingularAttribute<PortLldpInfoVO, String> interfaceType;
    public static volatile SingularAttribute<PortLldpInfoVO, String> bondIfName;
    public static volatile SingularAttribute<PortLldpInfoVO, String> portName;
    public static volatile SingularAttribute<PortLldpInfoVO, String> systemName;
    public static volatile SingularAttribute<PortLldpInfoVO, String> chassisMac;
    public static volatile SingularAttribute<PortLldpInfoVO, Boolean> aggregated;
    public static volatile SingularAttribute<PortLldpInfoVO, Integer> aggregatedPortID;
    public static volatile SingularAttribute<PortLldpInfoVO, Timestamp> createDate;
    public static volatile SingularAttribute<PortLldpInfoVO, Timestamp> lastOpDate;
}
