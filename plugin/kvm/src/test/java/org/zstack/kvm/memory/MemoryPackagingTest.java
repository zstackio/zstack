package org.zstack.kvm.memory;

import org.junit.Test;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;
import static org.junit.Assert.*;

/** The premium WAR overrides the community persistence unit; test both delivery paths. */
public class MemoryPackagingTest {
    @Test public void productionDeferredLockCleanupIsPresentInCompiledClass() throws Exception {
        // The Groovy compiler can overwrite woven main classes on a second
        // lifecycle run while ajc's source-based incremental check skips them.
        // Pure policy tests still pass, but this production class leaks GLock.
        String resource = "/org/zstack/kvm/hypervisor/KvmHypervisorInfoManagerImpl.class";
        try (InputStream input = getClass().getResourceAsStream(resource)) {
            assertNotNull(resource, input);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] block = new byte[8192];
            int length;
            while ((length = input.read(block)) != -1) bytes.write(block, 0, length);
            String constants = new String(bytes.toByteArray(), StandardCharsets.ISO_8859_1);
            assertTrue("production @Deferred methods must execute their cleanup advice",
                    constants.contains("ajc$after$org_zstack_core_defer_DeferAspect$4$"));
        }
    }

    @Test public void everyMemoryEntityIsRegisteredInBothDeliveryManifests() throws Exception {
        Path root = findRepositoryRoot(Paths.get(System.getProperty("user.dir")).toAbsolutePath());
        assertNotNull("repository root", root);
        for (String manifest : Arrays.asList("conf/persistence.xml", "premium/conf/persistence.xml")) {
            String xml = new String(Files.readAllBytes(root.resolve(manifest)), StandardCharsets.UTF_8);
            for (String entity : Arrays.asList("MemoryPolicyVO", "MemoryTaskVO", "MemoryTaskIdempotencyReceiptVO", "MemoryStateVO",
                    "MemoryMigrationVO", "MemoryVmExclusionVO", "MemoryTargetShardVO",
                    "MemoryTaskFailureOutboxVO", "MemoryCloudBootstrapVO", "MemoryQueryVersionVO",
                    "MemoryStandardConfigMigrationVO")) {
                assertTrue(manifest + " missing " + entity,
                        xml.contains("<class>org.zstack.kvm.memory." + entity + "</class>"));
            }
        }
    }

    private Path findRepositoryRoot(Path start) {
        Path current = start;
        while (current != null) {
            if (Files.isRegularFile(current.resolve("conf/persistence.xml"))
                    && Files.isRegularFile(current.resolve("premium/conf/persistence.xml"))
                    && Files.isRegularFile(current.resolve("plugin/kvm/pom.xml"))) {
                return current;
            }
            current = current.getParent();
        }
        return null;
    }

    @Test public void transactionalSqlBatchIsWovenInTheManagementArtifact() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/org/zstack/core/db/SQLBatchWithReturn.class")) {
            assertNotNull("SQLBatchWithReturn class", input);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] block = new byte[8192]; int length;
            while ((length = input.read(block)) != -1) bytes.write(block, 0, length);
            String constants = new String(bytes.toByteArray(), StandardCharsets.ISO_8859_1);
            assertTrue("SQLBatchWithReturn transaction advice must be woven",
                    constants.contains("ajc$around$org_springframework_transaction_aspectj_AbstractTransactionAspect"));
        }
    }
}
