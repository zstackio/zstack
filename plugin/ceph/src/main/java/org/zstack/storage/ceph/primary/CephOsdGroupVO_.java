package org.zstack.storage.ceph.primary;

import org.zstack.header.storage.primary.StorageCapacityAO_;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(CephOsdGroupVO.class)
public class CephOsdGroupVO_ extends StorageCapacityAO_ {
    public static volatile SingularAttribute<CephOsdGroupVO, String> uuid;
    public static volatile SingularAttribute<CephOsdGroupVO, String> primaryStorageUuid;
    public static volatile SingularAttribute<CephOsdGroupVO, String> osds;
    public static volatile SingularAttribute<CephOsdGroupVO, Long> availableCapacity;
    public static volatile SingularAttribute<CephOsdGroupVO, Long> availablePhysicalCapacity;
    public static volatile SingularAttribute<CephOsdGroupVO, Timestamp> createDate;
    public static volatile SingularAttribute<CephOsdGroupVO, Timestamp> lastOpDate;
}
