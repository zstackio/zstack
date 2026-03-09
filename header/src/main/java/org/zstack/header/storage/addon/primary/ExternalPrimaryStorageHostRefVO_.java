package org.zstack.header.storage.addon.primary;

import org.zstack.header.storage.primary.PrimaryStorageHostRefVO_;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(ExternalPrimaryStorageHostRefVO.class)
public class ExternalPrimaryStorageHostRefVO_ extends PrimaryStorageHostRefVO_ {
    public static volatile SingularAttribute<ExternalPrimaryStorageHostRefVO, Integer> hostId;
    public static volatile SingularAttribute<ExternalPrimaryStorageHostRefVO, String> protocol;
}
