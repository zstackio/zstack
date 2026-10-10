package org.zstack.kvm.memory;

import org.junit.Test;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.header.core.AsyncBackup;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceInventory;
import java.lang.reflect.Field;
import java.util.Arrays;
import static org.junit.Assert.*;

public class MemoryResourceCascadeTest {
    private static class RecordingRepository extends MemoryRepository {
        int cleanups;
        @Override public void cleanupDeletedResources() { cleanups++; }
    }
    private void inject(Object target, RecordingRepository repo) throws Exception {
        Field field = target.getClass().getDeclaredField("repository"); field.setAccessible(true); field.set(target, repo);
    }
    @Test public void onlyPostDeletionCleanupPhaseCleansMemoryRecords() throws Exception {
        MemoryResourceCascadeExtension extension = new MemoryResourceCascadeExtension();
        RecordingRepository repository = new RecordingRepository(); inject(extension, repository);
        assertEquals(Arrays.asList("HostVO", "ClusterVO", "VmInstanceVO"), extension.getEdgeNames());
        for (String code : Arrays.asList(CascadeConstant.DELETION_CHECK_CODE, CascadeConstant.DELETION_DELETE_CODE,
                CascadeConstant.DELETION_FORCE_DELETE_CODE, CascadeConstant.DELETION_CLEANUP_CODE)) {
            final boolean[] completed = {false};
            extension.asyncCascade(new CascadeAction().setActionCode(code), new Completion(new AsyncBackup() {}) {
                @Override public void success() { completed[0] = true; }
                @Override public void fail(ErrorCode error) { org.junit.Assert.fail(error.toString()); }
            });
            assertTrue(completed[0]);
            assertEquals(CascadeConstant.DELETION_CLEANUP_CODE.equals(code) ? 1 : 0, repository.cleanups);
        }
    }
    @Test public void vmCleanupWaitsForActualDatabaseRemoval() throws Exception {
        RecordingRepository repository = new RecordingRepository();
        MemoryLifecycleHooks hooks = new MemoryLifecycleHooks(); inject(hooks, repository);
        VmInstanceInventory vm = new VmInstanceInventory(); vm.setUuid("vm1");
        hooks.afterDestroyVm(vm); // A recoverable VM is still a resource.
        assertEquals(0, repository.cleanups);
        hooks.vmJustAfterDeleteFromDbExtensionPoint(vm, "account1");
        assertEquals(1, repository.cleanups);
    }
}
