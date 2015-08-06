package org.zstack.core.gc;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by frank on 8/5/2015.
 */
@StaticMetamodel(GarbageCollectorVO.class)
public class GarbageCollectorVO_ {
    public static volatile SingularAttribute<GarbageCollectorVO, Long> id;
    public static volatile SingularAttribute<GarbageCollectorVO, String> runnerClass;
    public static volatile SingularAttribute<GarbageCollectorVO, GCStatus> status;
    public static volatile SingularAttribute<GarbageCollectorVO, String> managementNodeUuid;
    public static volatile SingularAttribute<GarbageCollectorVO, Timestamp> createDate;
    public static volatile SingularAttribute<GarbageCollectorVO, Timestamp> lastopDate;
}
