package org.zstack.core.logging;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 */
@StaticMetamodel(LogVO.class)
public class LogVO_ {
    public static volatile SingularAttribute<LogVO, Long> id;
    public static volatile SingularAttribute<LogVO, String> content;
    public static volatile SingularAttribute<LogVO, String> resourceUuid;
    public static volatile SingularAttribute<LogVO, LogType> type;
    public static volatile SingularAttribute<LogVO, LogLevel> level;
    public static volatile SingularAttribute<LogVO, Timestamp> createDate;
    public static volatile SingularAttribute<LogVO, Timestamp> lastOpDate;
}
