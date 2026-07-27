package org.zstack.networksecuritypolicyschedule;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

@StaticMetamodel(NetworkSecurityPolicyScheduleVO.class)
public class NetworkSecurityPolicyScheduleVO_ {
    public static volatile SingularAttribute<NetworkSecurityPolicyScheduleVO, String> uuid;
    public static volatile SingularAttribute<NetworkSecurityPolicyScheduleVO, String> name;
    public static volatile SingularAttribute<NetworkSecurityPolicyScheduleVO, String> description;
    public static volatile SingularAttribute<NetworkSecurityPolicyScheduleVO, String> resourceType;
    public static volatile SingularAttribute<NetworkSecurityPolicyScheduleVO, String> resourceUuid;
    public static volatile SingularAttribute<NetworkSecurityPolicyScheduleVO, NetworkSecurityPolicyScheduleTimeType> timeType;
    public static volatile SingularAttribute<NetworkSecurityPolicyScheduleVO, NetworkSecurityPolicyScheduleRepeatType> repeatType;
    public static volatile SingularAttribute<NetworkSecurityPolicyScheduleVO, Date> startDate;
    public static volatile SingularAttribute<NetworkSecurityPolicyScheduleVO, Date> endDate;
    public static volatile SingularAttribute<NetworkSecurityPolicyScheduleVO, Time> startTime;
    public static volatile SingularAttribute<NetworkSecurityPolicyScheduleVO, Time> endTime;
    public static volatile SingularAttribute<NetworkSecurityPolicyScheduleVO, String> weekDays;
    public static volatile SingularAttribute<NetworkSecurityPolicyScheduleVO, Timestamp> createDate;
    public static volatile SingularAttribute<NetworkSecurityPolicyScheduleVO, Timestamp> lastOpDate;
}
