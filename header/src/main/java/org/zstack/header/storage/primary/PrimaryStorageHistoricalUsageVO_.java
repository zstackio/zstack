package org.zstack.header.storage.primary;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(PrimaryStorageHistoricalUsageVO.class)
public class PrimaryStorageHistoricalUsageVO_ extends HistoricalUsageAO_ {
    public static volatile SingularAttribute<PrimaryStorageHistoricalUsageVO, String> primaryStorageUuid;
}
