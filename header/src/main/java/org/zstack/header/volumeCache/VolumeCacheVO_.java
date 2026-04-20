package org.zstack.header.volumeCache;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(VolumeCacheVO.class)
public class VolumeCacheVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<VolumeCacheVO, String> volumeUuid;
    public static volatile SingularAttribute<VolumeCacheVO, String> poolUuid;
    public static volatile SingularAttribute<VolumeCacheVO, String> installPath;
    public static volatile SingularAttribute<VolumeCacheVO, VolumeCacheMode> cacheMode;
    public static volatile SingularAttribute<VolumeCacheVO, VolumeCacheStatus> status;
    public static volatile SingularAttribute<VolumeCacheVO, Timestamp> createDate;
    public static volatile SingularAttribute<VolumeCacheVO, Timestamp> lastOpDate;
}
