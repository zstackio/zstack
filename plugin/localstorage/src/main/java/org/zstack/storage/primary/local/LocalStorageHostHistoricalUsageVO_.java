package org.zstack.storage.primary.local;

import org.zstack.header.storage.primary.HistoricalUsageAO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(LocalStorageHostHistoricalUsageVO.class)
public class LocalStorageHostHistoricalUsageVO_ extends HistoricalUsageAO_ {
    public static volatile SingularAttribute<LocalStorageHostHistoricalUsageVO, String> primaryStorageUuid;
    public static volatile SingularAttribute<LocalStorageHostHistoricalUsageVO, String> hostUuid;
}
