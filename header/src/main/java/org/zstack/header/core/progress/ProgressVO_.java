package org.zstack.header.core.progress;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by mingjian.deng on 16/12/12.
 */
@StaticMetamodel(ProgressVO.class)
public class ProgressVO_ {
    public static volatile SingularAttribute<Long, String> id;
    public static volatile SingularAttribute<Long, String> processType;
    public static volatile SingularAttribute<Long, String> progress;
    public static volatile SingularAttribute<Long, String> resourceUuid;
    public static volatile SingularAttribute<Long, Timestamp> createDate;
    public static volatile SingularAttribute<Long, Timestamp> lastOpDate;
}
