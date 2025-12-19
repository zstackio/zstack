package org.zstack.header.storage.addon.primary;


import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(ExternalPrimaryStorageSpaceVO.class)
public class ExternalPrimaryStorageSpaceVO_ {

    public static volatile SingularAttribute<ExternalPrimaryStorageSpaceVO, String> uuid;
    public static volatile SingularAttribute<ExternalPrimaryStorageSpaceVO, String> primaryStorageUuid;
    public static volatile SingularAttribute<ExternalPrimaryStorageSpaceVO, String> type;
    public static volatile SingularAttribute<ExternalPrimaryStorageSpaceVO, String> name;
    public static volatile SingularAttribute<ExternalPrimaryStorageSpaceVO, String> locationUrl;
    public static volatile SingularAttribute<ExternalPrimaryStorageSpaceVO, Long> availableCapacity;
    public static volatile SingularAttribute<ExternalPrimaryStorageSpaceVO, Long> totalCapacity;
    public static volatile SingularAttribute<ExternalPrimaryStorageSpaceVO, Long> availablePhysicalCapacity;
    public static volatile SingularAttribute<ExternalPrimaryStorageSpaceVO, Long> totalPhysicalCapacity;
}
