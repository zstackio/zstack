package org.zstack.header.storage.primary;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(PrimaryStorageClusterRefVO.class)
public class PrimaryStorageClusterRefVO_ {
    public static volatile SingularAttribute<PrimaryStorageClusterRefVO, Long> id;
    public static volatile SingularAttribute<PrimaryStorageClusterRefVO, String> clusterUuid;
    public static volatile SingularAttribute<PrimaryStorageClusterRefVO, String> primaryStorageUuid;
}
