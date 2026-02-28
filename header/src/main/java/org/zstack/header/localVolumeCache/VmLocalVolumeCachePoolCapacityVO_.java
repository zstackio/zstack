package org.zstack.header.localVolumeCache;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(VmLocalVolumeCachePoolCapacityVO.class)
public class VmLocalVolumeCachePoolCapacityVO_ {
    public static volatile SingularAttribute<VmLocalVolumeCachePoolCapacityVO, String> uuid;
    public static volatile SingularAttribute<VmLocalVolumeCachePoolCapacityVO, Long> totalCapacity;
    public static volatile SingularAttribute<VmLocalVolumeCachePoolCapacityVO, Long> availableCapacity;
    public static volatile SingularAttribute<VmLocalVolumeCachePoolCapacityVO, Long> allocated;
    public static volatile SingularAttribute<VmLocalVolumeCachePoolCapacityVO, Long> dirty;
}
