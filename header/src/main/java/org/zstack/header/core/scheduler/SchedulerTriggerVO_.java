package org.zstack.header.core.scheduler;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by AlanJager on 2017/6/7.
 */
@StaticMetamodel(SchedulerTriggerVO_.class)
public class SchedulerTriggerVO_ {
    public static volatile SingularAttribute<SchedulerTriggerVO, String> name;
    public static volatile SingularAttribute<SchedulerTriggerVO, String> description;
    public static volatile SingularAttribute<SchedulerTriggerVO, String> type;
    public static volatile SingularAttribute<SchedulerTriggerVO, Integer> interval;
    public static volatile SingularAttribute<SchedulerTriggerVO, Integer> repeatCount;
    public static volatile SingularAttribute<SchedulerTriggerVO, String> managementNodeUuid;
    public static volatile SingularAttribute<SchedulerTriggerVO, Timestamp> createDate;
    public static volatile SingularAttribute<SchedulerTriggerVO, Timestamp> startTime;
    public static volatile SingularAttribute<SchedulerTriggerVO, Timestamp> stopTime;
    public static volatile SingularAttribute<SchedulerTriggerVO, Timestamp> lastOpDate;
}
