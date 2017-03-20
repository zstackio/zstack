package org.zstack.header.core.progress;

/**
 * Created by xing5 on 2017/3/23.
 */

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(TaskStepVO.class)
public class TaskStepVO_ {
    public static volatile SingularAttribute<TaskStepVO, Long> id;
    public static volatile SingularAttribute<TaskStepVO, String> taskName;
    public static volatile SingularAttribute<TaskStepVO, String> content;
    public static volatile SingularAttribute<TaskStepVO, Timestamp> createDate;
    public static volatile SingularAttribute<TaskStepVO, Timestamp> lastOpDate;
}
