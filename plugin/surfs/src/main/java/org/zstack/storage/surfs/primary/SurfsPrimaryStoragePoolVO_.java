package org.zstack.storage.surfs.primary;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by zhouhaiping 2017-11-24
 */
@StaticMetamodel(SurfsPrimaryStoragePoolVO.class)
public class SurfsPrimaryStoragePoolVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<SurfsPrimaryStoragePoolVO, String> primaryStorageUuid;
    public static volatile SingularAttribute<SurfsPrimaryStoragePoolVO, String> poolName;
    public static volatile SingularAttribute<SurfsPrimaryStoragePoolVO, String> aliasName;
    public static volatile SingularAttribute<SurfsPrimaryStoragePoolVO, String> description;
    public static volatile SingularAttribute<SurfsPrimaryStoragePoolVO, Timestamp> createDate;
    public static volatile SingularAttribute<SurfsPrimaryStoragePoolVO, Timestamp> lastOpDate;
    public static volatile SingularAttribute<SurfsPrimaryStoragePoolVO, String> type;
}
