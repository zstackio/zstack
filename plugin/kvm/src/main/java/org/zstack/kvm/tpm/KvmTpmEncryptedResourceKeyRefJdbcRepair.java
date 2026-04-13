package org.zstack.kvm.tpm;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.keyprovider.EncryptedResourceKeyRefVO;
import org.zstack.header.keyprovider.EncryptedResourceKeyRefVO_;
import org.zstack.header.tpm.entity.TpmVO;

/**
 * Per-TPM EncryptedResourceKeyRef native repair; separate bean so {@code REQUIRES_NEW} applies without self-proxy.
 */
public class KvmTpmEncryptedResourceKeyRefJdbcRepair {

    /**
     * Remove duplicate placeholder rows for this TPM: provider bound but KEK not materialized yet.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int deleteOrphanPlaceholderTpmKeyRefRows(String tpmUuid) {
        Long paired = Q.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.resourceType, TpmVO.class.getSimpleName())
                .eq(EncryptedResourceKeyRefVO_.resourceUuid, tpmUuid)
                .notNull(EncryptedResourceKeyRefVO_.kekRef)
                .count();
        if (paired == null || paired.longValue() <= 0) {
            return 0;
        }

        return SQL.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.resourceType, TpmVO.class.getSimpleName())
                .eq(EncryptedResourceKeyRefVO_.resourceUuid, tpmUuid)
                .notNull(EncryptedResourceKeyRefVO_.providerUuid)
                .isNull(EncryptedResourceKeyRefVO_.kekRef)
                .delete();
    }

    /**
     * After NKP restore, {@code providerUuid} may be NULL (FK) while {@code kekRef} still holds wrapped key material.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int applyProviderUuidOnRowWithKek(String tpmUuid, String providerUuid) {
        return SQL.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.resourceType, TpmVO.class.getSimpleName())
                .eq(EncryptedResourceKeyRefVO_.resourceUuid, tpmUuid)
                .isNull(EncryptedResourceKeyRefVO_.providerUuid)
                .notNull(EncryptedResourceKeyRefVO_.kekRef)
                .set(EncryptedResourceKeyRefVO_.providerUuid, providerUuid)
                .set(EncryptedResourceKeyRefVO_.lastOpDate, null)
                .update();
    }

    @Transactional(readOnly = true)
    public boolean hasAnyRefRowForTpm(String tpmUuid) {
        Long count = Q.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.resourceType, TpmVO.class.getSimpleName())
                .eq(EncryptedResourceKeyRefVO_.resourceUuid, tpmUuid)
                .count();
        return count != null && count.longValue() > 0;
    }
}

