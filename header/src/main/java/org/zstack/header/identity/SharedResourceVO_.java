package org.zstack.header.identity;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by frank on 7/13/2015.
 */
@StaticMetamodel(SharedResourceVO.class)
public class SharedResourceVO_ {
    public static volatile SingularAttribute<SharedResourceVO, Long> id;
    public static volatile SingularAttribute<SharedResourceVO, String> ownerAccountUuid;
    public static volatile SingularAttribute<SharedResourceVO, String> receiverAccountUuid;
    public static volatile SingularAttribute<SharedResourceVO, Boolean> toPublic;
    public static volatile SingularAttribute<SharedResourceVO, String> resourceUuid;
    public static volatile SingularAttribute<SharedResourceVO, String> resourceType;
    public static volatile SingularAttribute<SharedResourceVO, Timestamp> lastOpDate;
    public static volatile SingularAttribute<SharedResourceVO, Timestamp> createDate;
}
