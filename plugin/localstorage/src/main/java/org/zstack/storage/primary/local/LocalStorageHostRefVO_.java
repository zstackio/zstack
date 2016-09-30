package org.zstack.storage.primary.local;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by frank on 6/30/2015.
 */
@StaticMetamodel(LocalStorageHostRefVO.class)
public class LocalStorageHostRefVO_ {
    public static volatile SingularAttribute<LocalStorageHostRefVO, String> primaryStorageUuid;
    public static volatile SingularAttribute<LocalStorageHostRefVO, String> hostUuid;
    public static volatile SingularAttribute<LocalStorageHostRefVO, Timestamp> createDate;
    public static volatile SingularAttribute<LocalStorageHostRefVO, Timestamp> lastOpDate;
    public static volatile SingularAttribute<LocalStorageHostRefVO, Long> totalCapacity;
    public static volatile SingularAttribute<LocalStorageHostRefVO, Long> availableCapacity;
    public static volatile SingularAttribute<LocalStorageHostRefVO, Long> availablePhysicalCapacity;
    public static volatile SingularAttribute<LocalStorageHostRefVO, Long> totalPhysicalCapacity;
}
