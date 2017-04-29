package org.zstack.header.identity;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(PolicyVO.class)
public class PolicyVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<PolicyVO, String> name;
    public static volatile SingularAttribute<PolicyVO, String> description;
    public static volatile SingularAttribute<PolicyVO, String> accountUuid;
    public static volatile SingularAttribute<PolicyVO, String> type;
    public static volatile SingularAttribute<PolicyVO, String> data;
    public static volatile SingularAttribute<PolicyVO, Timestamp> createDate;
}
