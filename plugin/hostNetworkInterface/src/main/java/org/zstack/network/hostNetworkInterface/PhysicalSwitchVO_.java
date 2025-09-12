package org.zstack.network.hostNetworkInterface;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Physical Switch VO Metamodel
 */
@StaticMetamodel(PhysicalSwitchVO.class)
public class PhysicalSwitchVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<PhysicalSwitchVO, String> name;
    public static volatile SingularAttribute<PhysicalSwitchVO, String> description;
    public static volatile SingularAttribute<PhysicalSwitchVO, String> ip;
    public static volatile SingularAttribute<PhysicalSwitchVO, String> mac;
    public static volatile SingularAttribute<PhysicalSwitchVO, String> mode;
    public static volatile SingularAttribute<PhysicalSwitchVO, String> softwareVersion;
    public static volatile SingularAttribute<PhysicalSwitchVO, String> sdnControllerUuid;
    public static volatile SingularAttribute<PhysicalSwitchVO, Timestamp> createDate;
    public static volatile SingularAttribute<PhysicalSwitchVO, Timestamp> lastOpDate;
}
