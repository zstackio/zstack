package org.zstack.header.core.progress;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by xing5 on 2017/3/20.
 */
@StaticMetamodel(TaskProgressVO.class)
public class TaskProgressVO_ {
    public static volatile SingularAttribute<TaskProgressVO, Long> id;
    public static volatile SingularAttribute<TaskProgressVO, String> taskUuid;
    public static volatile SingularAttribute<TaskProgressVO, String> apiId;
    public static volatile SingularAttribute<TaskProgressVO, String> taskName;
    public static volatile SingularAttribute<TaskProgressVO, String> parentUuid;
    public static volatile SingularAttribute<TaskProgressVO, TaskType> type;
    public static volatile SingularAttribute<TaskProgressVO, String> content;
    public static volatile SingularAttribute<TaskProgressVO, String> arguments;
    public static volatile SingularAttribute<TaskProgressVO, String> managementUuid;
    public static volatile SingularAttribute<TaskProgressVO, Long> timeToDelete;
    public static volatile SingularAttribute<TaskProgressVO, Long> time;
}
