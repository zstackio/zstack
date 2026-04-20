package org.zstack.header.volumeCache;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(HostCacheStoreVO.class)
public class HostCacheStoreVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<HostCacheStoreVO, String> hostUuid;
    public static volatile SingularAttribute<HostCacheStoreVO, String> name;
    public static volatile SingularAttribute<HostCacheStoreVO, String> description;
    public static volatile SingularAttribute<HostCacheStoreVO, String> mountPoint;
    public static volatile SingularAttribute<HostCacheStoreVO, HostCacheStoreState> state;
    public static volatile SingularAttribute<HostCacheStoreVO, HostCacheStoreStatus> status;
    public static volatile SingularAttribute<HostCacheStoreVO, Timestamp> createDate;
    public static volatile SingularAttribute<HostCacheStoreVO, Timestamp> lastOpDate;
}
