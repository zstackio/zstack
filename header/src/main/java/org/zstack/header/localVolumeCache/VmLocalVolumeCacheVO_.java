package org.zstack.header.localVolumeCache;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(VmLocalVolumeCacheVO.class)
public class VmLocalVolumeCacheVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<VmLocalVolumeCacheVO, String> volumeUuid;
    public static volatile SingularAttribute<VmLocalVolumeCacheVO, String> poolUuid;
    public static volatile SingularAttribute<VmLocalVolumeCacheVO, String> installPath;
    public static volatile SingularAttribute<VmLocalVolumeCacheVO, VmLocalVolumeCacheMode> cacheMode;
    public static volatile SingularAttribute<VmLocalVolumeCacheVO, VmLocalVolumeCacheState> state;
    public static volatile SingularAttribute<VmLocalVolumeCacheVO, Timestamp> createDate;
    public static volatile SingularAttribute<VmLocalVolumeCacheVO, Timestamp> lastOpDate;
}
