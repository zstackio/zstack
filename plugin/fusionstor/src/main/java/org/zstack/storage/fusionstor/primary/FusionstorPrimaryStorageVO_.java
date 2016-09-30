package org.zstack.storage.fusionstor.primary;

import org.zstack.header.storage.primary.PrimaryStorageVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * Created by frank on 7/29/2015.
 */
@StaticMetamodel(FusionstorPrimaryStorageVO.class)
public class FusionstorPrimaryStorageVO_ extends PrimaryStorageVO_ {
    public static volatile SingularAttribute<FusionstorPrimaryStorageVO, String> fsid;
    public static volatile SingularAttribute<FusionstorPrimaryStorageVO, String> rootVolumePoolName;
    public static volatile SingularAttribute<FusionstorPrimaryStorageVO, String> dataVolumePoolName;
    public static volatile SingularAttribute<FusionstorPrimaryStorageVO, String> imageCachePoolName;
    public static volatile SingularAttribute<FusionstorPrimaryStorageVO, String> userKey;
}
