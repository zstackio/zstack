package org.zstack.core.apicost;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(APIHistoryVO.class)
public class APIHistoryVO_ {
    public static volatile SingularAttribute<APIHistoryVO, String> requestUuid;
    public static volatile SingularAttribute<APIHistoryVO, String> apiName;
    public static volatile SingularAttribute<APIHistoryVO, String> requestDump;
    public static volatile SingularAttribute<APIHistoryVO, String> responseDump;
    public static volatile SingularAttribute<APIHistoryVO, Timestamp> requestDate;
    public static volatile SingularAttribute<APIHistoryVO, Timestamp> responseDate;
}
