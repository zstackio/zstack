package org.zstack.network.hostNetworkInterface;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Physical Switch Port VO Metamodel
 */
@StaticMetamodel(PhysicalSwitchPortVO.class)
public class PhysicalSwitchPortVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<PhysicalSwitchPortVO, String> name;
    public static volatile SingularAttribute<PhysicalSwitchPortVO, String> description;
    public static volatile SingularAttribute<PhysicalSwitchPortVO, String> ethTrunkName;
    public static volatile SingularAttribute<PhysicalSwitchPortVO, String> portType;
    public static volatile SingularAttribute<PhysicalSwitchPortVO, String> peerInterfaceUuid;
    public static volatile SingularAttribute<PhysicalSwitchPortVO, String> switchUuid;
    public static volatile SingularAttribute<PhysicalSwitchPortVO, Timestamp> createDate;
    public static volatile SingularAttribute<PhysicalSwitchPortVO, Timestamp> lastOpDate;
}
