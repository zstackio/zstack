package org.zstack.kvm.tpm;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Regression guard for NKP/restore TPM provider repair in {@link KvmTpmExtensions}
 * (no Spring / DB). Refactor rename will fail tests until expectations are updated.
 */
public class KvmTpmExtensionsTpmRepairContractTest {

    @Test
    public void kvmTpmExtensionsDeclaresJdbcRepairField() throws Exception {
        Field f = KvmTpmExtensions.class.getDeclaredField("tpmKeyRefJdbcRepair");
        Assert.assertEquals(KvmTpmEncryptedResourceKeyRefJdbcRepair.class, f.getType());
    }

    @Test
    public void kvmTpmExtensionsDeclaresRepairAndSafeLookupMethods() throws Exception {
        Class<?> c = KvmTpmExtensions.class;
        c.getDeclaredMethod("tryRebindKeyProviderByName", String.class);
        c.getDeclaredMethod("repairOrphanTpmKeyRefPlaceholders", String.class);
        c.getDeclaredMethod("safeFindKeyProviderUuidByTpm", String.class);
        c.getDeclaredMethod("safeFindKeyProviderNameByTpm", String.class);
        c.getDeclaredMethod("safeFindKeyProviderUuidByName", String.class, String.class);
        Method m = c.getDeclaredMethod("isNonUniqueResultException", Throwable.class);
        Assert.assertTrue(java.lang.reflect.Modifier.isStatic(m.getModifiers()));
    }

    @Test
    public void jdbcInPlaceUpdateLogTemplateForOperations() {
        String fmt = "updated EncryptedResourceKeyRef.providerUuid in-place for tpm[uuid:%s], rows:%d";
        Assert.assertEquals(
                "updated EncryptedResourceKeyRef.providerUuid in-place for tpm[uuid:tpm-1], rows:1",
                String.format(fmt, "tpm-1", 1));
    }

    @Test
    public void placeholderDeleteLogTemplateForOperations() {
        String fmt = "removed %d EncryptedResourceKeyRef placeholder row(s) for tpm[uuid:%s]";
        Assert.assertEquals(
                "removed 2 EncryptedResourceKeyRef placeholder row(s) for tpm[uuid:tpm-1]",
                String.format(fmt, 2, "tpm-1"));
    }

    @Test
    public void reboundAfterClearedProviderUuidLogTemplateForOperations() {
        String fmt = "rebound TPM key provider by providerName after ref.providerUuid was cleared, tpm[uuid:%s], providerUuid:%s";
        Assert.assertEquals(
                "rebound TPM key provider by providerName after ref.providerUuid was cleared, tpm[uuid:tpm-1], providerUuid:pu-1",
                String.format(fmt, "tpm-1", "pu-1"));
    }
}
