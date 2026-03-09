package org.zstack.core.job;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.util.Date;


@StaticMetamodel(JobQueueVO.class)
public class JobQueueVO_ {
    public static volatile SingularAttribute<JobQueueVO, Long> id;
    public static volatile SingularAttribute<JobQueueVO, String> name;
    public static volatile SingularAttribute<JobQueueVO, String> owner;
    public static volatile SingularAttribute<JobQueueVO, String> workerManagementNodeId;
    public static volatile SingularAttribute<JobQueueVO, Date> takenDate;
}
