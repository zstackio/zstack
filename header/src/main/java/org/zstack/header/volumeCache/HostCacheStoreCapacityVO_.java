package org.zstack.header.volumeCache;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(HostCacheStoreCapacityVO.class)
public class HostCacheStoreCapacityVO_ {
    public static volatile SingularAttribute<HostCacheStoreCapacityVO, String> uuid;
    public static volatile SingularAttribute<HostCacheStoreCapacityVO, Long> totalCapacity;
    public static volatile SingularAttribute<HostCacheStoreCapacityVO, Long> availableCapacity;
    public static volatile SingularAttribute<HostCacheStoreCapacityVO, Long> totalPhysicalCapacity;
    public static volatile SingularAttribute<HostCacheStoreCapacityVO, Long> availablePhysicalCapacity;
    public static volatile SingularAttribute<HostCacheStoreCapacityVO, Long> systemUsedCapacity;
}
