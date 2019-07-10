package org.zstack.storage.surfs.primary;

import org.zstack.header.storage.primary.PrimaryStorageVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created by zhouhaiping 2017-09-13
 */
@StaticMetamodel(SurfsPrimaryStorageVO.class)
public class SurfsPrimaryStorageVO_ extends PrimaryStorageVO_ {
    public static volatile SingularAttribute<SurfsPrimaryStorageVO, String> fsid;
    public static volatile SingularAttribute<SurfsPrimaryStorageVO, String> rootVolumePoolName;
    public static volatile SingularAttribute<SurfsPrimaryStorageVO, String> dataVolumePoolName;
    public static volatile SingularAttribute<SurfsPrimaryStorageVO, String> imageCachePoolName;
    public static volatile SingularAttribute<SurfsPrimaryStorageVO, String> userKey;
}
