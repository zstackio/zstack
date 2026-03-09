package org.zstack.network.securitygroup;

import org.zstack.header.vo.ResourceVO_;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(SecurityGroupVO.class)
public class SecurityGroupVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<SecurityGroupVO, String> name;
    public static volatile SingularAttribute<SecurityGroupVO, String> description;
    public static volatile SingularAttribute<SecurityGroupVO, SecurityGroupState> state;
    public static volatile SingularAttribute<SecurityGroupVO, Long> internalId;
    public static volatile SingularAttribute<SecurityGroupVO, Integer> ipVersion;
    public static volatile SingularAttribute<SecurityGroupVO, String> vSwitchType;
    public static volatile SingularAttribute<SecurityGroupVO, Timestamp> createDate;
    public static volatile SingularAttribute<SecurityGroupVO, Timestamp> lastOpDate;
}
