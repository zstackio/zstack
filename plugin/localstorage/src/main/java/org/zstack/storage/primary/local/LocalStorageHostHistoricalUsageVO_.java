package org.zstack.storage.primary.local;

import org.zstack.header.storage.primary.PrimaryStorageHistoricalUsageBaseVO_;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(LocalStorageHostHistoricalUsageVO.class)
public class LocalStorageHostHistoricalUsageVO_ extends PrimaryStorageHistoricalUsageBaseVO_ {
    public static volatile SingularAttribute<LocalStorageHostHistoricalUsageVO, String> hostUuid;
}
