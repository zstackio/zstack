package org.zstack.kvm.tpm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.tpm.entity.TpmVO;

import javax.persistence.Query;

/**
 * Per-TPM EncryptedResourceKeyRef native repair; separate bean so {@code REQUIRES_NEW} applies without self-proxy.
 */
public class KvmTpmEncryptedResourceKeyRefJdbcRepair {

    @Autowired
    private DatabaseFacade databaseFacade;

    /**
     * Remove duplicate placeholder rows for this TPM: provider bound but KEK not materialized yet.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int deleteOrphanPlaceholderTpmKeyRefRows(String tpmUuid) {
        Query q = databaseFacade.getEntityManager().createNativeQuery(
                "DELETE FROM EncryptedResourceKeyRefVO WHERE resourceType = :rt AND resourceUuid = :tu "
                        + "AND providerUuid IS NOT NULL AND kekRef IS NULL");
        q.setParameter("rt", TpmVO.class.getSimpleName());
        q.setParameter("tu", tpmUuid);
        return q.executeUpdate();
    }

    /**
     * After NKP restore, {@code providerUuid} may be NULL (FK) while {@code kekRef} still holds wrapped key material.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int applyProviderUuidOnRowWithKek(String tpmUuid, String providerUuid) {
        Query q = databaseFacade.getEntityManager().createNativeQuery(
                "UPDATE EncryptedResourceKeyRefVO SET providerUuid = :pu, lastOpDate = CURRENT_TIMESTAMP(3) "
                        + "WHERE resourceType = :rt AND resourceUuid = :tu AND providerUuid IS NULL AND kekRef IS NOT NULL");
        q.setParameter("pu", providerUuid);
        q.setParameter("rt", TpmVO.class.getSimpleName());
        q.setParameter("tu", tpmUuid);
        return q.executeUpdate();
    }
}
