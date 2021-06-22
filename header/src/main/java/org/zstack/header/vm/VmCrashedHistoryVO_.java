package org.zstack.header.vm;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by Wenhao.Zhang on 21/06/22
 */
@StaticMetamodel(VmCrashedHistoryVO.class)
public class VmCrashedHistoryVO_ {
    public static volatile SingularAttribute<VmCrashedHistoryVO, Long> id;
    public static volatile SingularAttribute<VmCrashedHistoryVO, String> uuid;
    public static volatile SingularAttribute<VmCrashedHistoryVO, Long> dateInLong;
    public static volatile SingularAttribute<VmCrashedHistoryVO, Timestamp> createDate;
    public static volatile SingularAttribute<VmCrashedHistoryVO, Timestamp> lastOpDate;
}
