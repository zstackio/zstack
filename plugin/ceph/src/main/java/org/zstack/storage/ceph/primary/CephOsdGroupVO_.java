package org.zstack.storage.ceph.primary;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(CephOsdGroupVO.class)
public class CephOsdGroupVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<CephOsdGroupVO, String> primaryStorageUuid;
    public static volatile SingularAttribute<CephOsdGroupVO, String> osds;
    public static volatile SingularAttribute<CephOsdGroupVO, Long> availableCapacity;
    public static volatile SingularAttribute<CephOsdGroupVO, Long> availablePhysicalCapacity;
    public static volatile SingularAttribute<CephOsdGroupVO, Long> usedPhysicalCapacity;
    public static volatile SingularAttribute<CephOsdGroupVO, Long> totalPhysicalCapacity;
    public static volatile SingularAttribute<CephOsdGroupVO, Timestamp> createDate;
    public static volatile SingularAttribute<CephOsdGroupVO, Timestamp> lastOpDate;
}
