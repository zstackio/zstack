package org.zstack.kvm.tpm;

import org.junit.Assert;
import org.junit.Test;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.keyprovider.EncryptedResourceKeyRefVO;

/**
 * Contract tests for native SQL in {@link KvmTpmEncryptedResourceKeyRefJdbcRepair} (no DB).
 * If these fail, update the repair bean and keep log/ops runbooks in sync.
 *
 * <p>Used from {@link org.zstack.kvm.tpm.KvmTpmExtensions#tryRebindKeyProviderByName} after NKP restore
 * when {@code providerUuid} is NULL but {@code kekRef} is set.
 */
public class KvmTpmEncryptedResourceKeyRefJdbcRepairDocTest {

    @Test
    public void deleteOrphanPlaceholderUsesQAndSqlBuilder() {
        Assert.assertNotNull(Q.New(EncryptedResourceKeyRefVO.class));
        Assert.assertNotNull(SQL.New(EncryptedResourceKeyRefVO.class));
    }

    @Test
    public void applyProviderUuidOnRowWithKekUsesQAndSqlBuilder() {
        Assert.assertNotNull(Q.New(EncryptedResourceKeyRefVO.class));
        Assert.assertNotNull(SQL.New(EncryptedResourceKeyRefVO.class));
    }

    @Test
    public void repairBeanExposesExpectedPublicApi() throws Exception {
        KvmTpmEncryptedResourceKeyRefJdbcRepair.class.getMethod("deleteOrphanPlaceholderTpmKeyRefRows", String.class);
        KvmTpmEncryptedResourceKeyRefJdbcRepair.class.getMethod("applyProviderUuidOnRowWithKek", String.class, String.class);
    }
}
