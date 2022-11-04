package org.zstack.storage.snapshot;

import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.header.identity.quota.QuotaDefinition;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;

public class VolumeSnapshotNumQuotaDefinition implements QuotaDefinition {
    @Override
    public String getName() {
        return VolumeSnapshotQuotaConstant.VOLUME_SNAPSHOT_NUM;
    }

    @Override
    public Long getDefaultValue() {
        return VolumeSnapshotQuotaGlobalConfig.VOLUME_SNAPSHOT_NUM.defaultValue(Long.class);
    }

    @Override
    public Long getQuotaUsage(String accountUuid) {
        return Q.New(AccountResourceRefVO.class)
                .eq(AccountResourceRefVO_.accountUuid, accountUuid)
                .eq(AccountResourceRefVO_.resourceType, VolumeSnapshotVO.class.getSimpleName())
                .count();
    }
}
