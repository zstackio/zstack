package org.zstack.header.core.progress;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by mingjian.deng on 16/12/12.
 */
@StaticMetamodel(ProgressVO.class)
public class ProgressVO_ {
    public static volatile SingularAttribute<ProgressVO, Long> id;
    public static volatile SingularAttribute<ProgressVO, String> processType;
    public static volatile SingularAttribute<ProgressVO, String> progress;
    public static volatile SingularAttribute<ProgressVO, String> resourceUuid;
    public static volatile SingularAttribute<ProgressVO, Timestamp> createDate;
    public static volatile SingularAttribute<ProgressVO, Timestamp> lastOpDate;
}
