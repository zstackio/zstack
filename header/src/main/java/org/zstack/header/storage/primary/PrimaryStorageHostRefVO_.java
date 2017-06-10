package org.zstack.header.storage.primary;

import javax.persistence.metamodel.SingularAttribute;
import java.sql.Timestamp;

/**
 * Created by Administrator on 2017-05-08.
 */
public class PrimaryStorageHostRefVO_ {
    public static volatile SingularAttribute<PrimaryStorageHostRefVO, String> hostUuid;
    public static volatile SingularAttribute<PrimaryStorageHostRefVO, String> primaryStorageUuid;
    public static volatile SingularAttribute<PrimaryStorageHostRefVO, PrimaryStorageHostStatus> status;
    public static volatile SingularAttribute<PrimaryStorageHostRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<PrimaryStorageHostRefVO, Timestamp> lastOpDate;
}
