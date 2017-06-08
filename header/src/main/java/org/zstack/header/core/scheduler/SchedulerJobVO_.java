package org.zstack.header.core.scheduler;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by AlanJager on 2017/6/7.
 */

@StaticMetamodel(SchedulerJobVO.class)
public class SchedulerJobVO_ {
    public static volatile SingularAttribute<SchedulerJobVO, String> uuid;
    public static volatile SingularAttribute<SchedulerJobVO, String> targetResourceUuid;
    public static volatile SingularAttribute<SchedulerJobVO, String> name;
    public static volatile SingularAttribute<SchedulerJobVO, String> description;
    public static volatile SingularAttribute<SchedulerJobVO, String> jobClassName;
    public static volatile SingularAttribute<SchedulerJobVO, String> jobData;
    public static volatile SingularAttribute<SchedulerJobVO, String> managementNodeUuid;
    public static volatile SingularAttribute<SchedulerJobVO, Timestamp> createDate;
    public static volatile SingularAttribute<SchedulerJobVO, Timestamp> lastOpDate;
}
