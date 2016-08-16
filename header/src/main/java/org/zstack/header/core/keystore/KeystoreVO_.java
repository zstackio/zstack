package org.zstack.header.core.keystore;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by miao on 16-8-15.
 */
@StaticMetamodel(KeystoreVO.class)
public class KeystoreVO_ {
    public static volatile SingularAttribute<KeystoreVO, String> uuid;
    public static volatile SingularAttribute<KeystoreVO, String> resourceUuid;
    public static volatile SingularAttribute<KeystoreVO, String> resourceType;
    public static volatile SingularAttribute<KeystoreVO, String> type;
    public static volatile SingularAttribute<KeystoreVO, String> content;
    public static volatile SingularAttribute<KeystoreVO, Timestamp> createDate;
    public static volatile SingularAttribute<KeystoreVO, Timestamp> lastOpDate;

}
