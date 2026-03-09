package org.zstack.header.storage.primary;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(PrimaryStorageHistoricalUsageBaseVO.class)
public class PrimaryStorageHistoricalUsageBaseVO_ extends HistoricalUsageAO_ {
    public static volatile SingularAttribute<PrimaryStorageHistoricalUsageBaseVO, String> primaryStorageUuid;
}
