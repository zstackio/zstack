package org.zstack.storage.ceph.primary;

import org.zstack.header.storage.primary.PrimaryStorageHistoricalUsageBaseVO_;

import jakarta.persistence.metamodel.SingularAttribute;
import jakarta.persistence.metamodel.StaticMetamodel;

@StaticMetamodel(CephOsdGroupHistoricalUsageVO.class)
public class CephOsdGroupHistoricalUsageVO_ extends PrimaryStorageHistoricalUsageBaseVO_ {
    public static volatile SingularAttribute<CephOsdGroupHistoricalUsageVO, String> osdGroupUuid;
}
