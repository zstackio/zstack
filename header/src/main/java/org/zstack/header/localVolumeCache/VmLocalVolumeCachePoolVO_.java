package org.zstack.header.localVolumeCache;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(VmLocalVolumeCachePoolVO.class)
public class VmLocalVolumeCachePoolVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<VmLocalVolumeCachePoolVO, String> hostUuid;
    public static volatile SingularAttribute<VmLocalVolumeCachePoolVO, String> name;
    public static volatile SingularAttribute<VmLocalVolumeCachePoolVO, String> description;
    public static volatile SingularAttribute<VmLocalVolumeCachePoolVO, String> metadata;
    public static volatile SingularAttribute<VmLocalVolumeCachePoolVO, VmLocalVolumeCachePoolState> state;
    public static volatile SingularAttribute<VmLocalVolumeCachePoolVO, VmLocalVolumeCachePoolStatus> status;
    public static volatile SingularAttribute<VmLocalVolumeCachePoolVO, Timestamp> createDate;
    public static volatile SingularAttribute<VmLocalVolumeCachePoolVO, Timestamp> lastOpDate;
}
