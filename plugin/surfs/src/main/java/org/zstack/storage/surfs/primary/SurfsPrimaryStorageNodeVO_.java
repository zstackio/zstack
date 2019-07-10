package org.zstack.storage.surfs.primary;

/**
 * Created by zhouhaipng
 */

import org.zstack.storage.surfs.SurfsNodeAO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(SurfsPrimaryStorageNodeVO.class)
public class SurfsPrimaryStorageNodeVO_ extends SurfsNodeAO_ {
    public static volatile SingularAttribute<SurfsPrimaryStorageNodeVO, String> primaryStorageUuid;
}
