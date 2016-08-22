package org.zstack.header.core.scheduler;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by Mei Lei on 7/13/16.
 */
@StaticMetamodel(SchedulerVO.class)
public class SchedulerVO_ {
    public static volatile SingularAttribute<SchedulerVO, String> uuid;
    public static volatile SingularAttribute<SchedulerVO, String> targetResourceUuid;
    public static volatile SingularAttribute<SchedulerVO, String> schedulerName;
    public static volatile SingularAttribute<SchedulerVO, String> schedulerDescription;
    public static volatile SingularAttribute<SchedulerVO, String> schedulerType;
    public static volatile SingularAttribute<SchedulerVO, Integer> schedulerInterval;
    public static volatile SingularAttribute<SchedulerVO, Integer> repeatCount;
    public static volatile SingularAttribute<SchedulerVO, String> cronScheduler;
    public static volatile SingularAttribute<SchedulerVO, String> jobName;
    public static volatile SingularAttribute<SchedulerVO, String> jobGroup;
    public static volatile SingularAttribute<SchedulerVO, String> triggerName;
    public static volatile SingularAttribute<SchedulerVO, String> triggerGroup;
    public static volatile SingularAttribute<SchedulerVO, String> jobClassName;
    public static volatile SingularAttribute<SchedulerVO, String> jobData;
    public static volatile SingularAttribute<SchedulerVO, SchedulerStatus> status;
    public static volatile SingularAttribute<SchedulerVO, String> managementNodeUuid;
    public static volatile SingularAttribute<SchedulerVO, Timestamp> createDate;
    public static volatile SingularAttribute<SchedulerVO, Timestamp> startDate;
    public static volatile SingularAttribute<SchedulerVO, Timestamp> stopDate;
    public static volatile SingularAttribute<SchedulerVO, Timestamp> lastOpDate;

}
