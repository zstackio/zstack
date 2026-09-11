package org.zstack.test.unittest.sdnController;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.CpuArchitecture;
import org.zstack.header.host.HostInventory;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMHostConnectedContext;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.sdnController.ZnsProxyGlobalProperty;
import org.zstack.sdnController.znsproxy.ZnsProxyInstaller;
import org.zstack.sdnController.znsproxy.ZnsProxyKvmReconnectExtension;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ZnsProxyInstallerTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private String originalPackageRepositoryPath;
    private String originalProxyPackageName;
    private String originalAnsibleModulePath;

    @Before
    public void setUp() {
        originalPackageRepositoryPath = ZnsProxyGlobalProperty.PACKAGE_REPOSITORY_PATH;
        originalProxyPackageName = ZnsProxyGlobalProperty.PROXY_PACKAGE_NAME;
        originalAnsibleModulePath = ZnsProxyGlobalProperty.ANSIBLE_MODULE_PATH;
        ZnsProxyGlobalProperty.PROXY_PACKAGE_NAME = "zns-proxy.bin";
    }

    @After
    public void tearDown() {
        ZnsProxyGlobalProperty.PACKAGE_REPOSITORY_PATH = originalPackageRepositoryPath;
        ZnsProxyGlobalProperty.PROXY_PACKAGE_NAME = originalProxyPackageName;
        ZnsProxyGlobalProperty.ANSIBLE_MODULE_PATH = originalAnsibleModulePath;
    }

    @Test
    public void testReconnectEnsuresKvmHost() throws Exception {
        TestZnsProxyKvmReconnectExtension extension = new TestZnsProxyKvmReconnectExtension();

        extension.connectionReestablished(kvmHost("host-uuid"));

        assertEquals(1, extension.ensureCount);
        assertEquals("host-uuid", extension.lastEnsuredHostUuid);
    }

    @Test
    public void testReconnectPreservesPackageFailureDetails() throws Exception {
        TestZnsProxyKvmReconnectExtension extension = new TestZnsProxyKvmReconnectExtension();
        extension.ensureFailure = new CloudRuntimeException("zns-proxy package checksum mismatch");

        try {
            extension.connectionReestablished(kvmHost("host-uuid"));
        } catch (OperationFailureException e) {
            assertEquals("zns-proxy package checksum mismatch", e.getErrorCode().getDetails());
            return;
        }
        throw new AssertionError("reconnect must fail when zns-proxy package validation fails");
    }

    @Test
    public void testReconnectSkipsNonKvmHost() throws Exception {
        TestZnsProxyKvmReconnectExtension extension = new TestZnsProxyKvmReconnectExtension();
        HostInventory host = kvmHost("host-uuid");
        host.setHypervisorType("Simulator");

        extension.connectionReestablished(host);

        assertEquals(0, extension.ensureCount);
    }

    @Test
    public void testReconnectSkipsUnsupportedHostArchitecture() throws Exception {
        TestZnsProxyKvmReconnectExtension extension = new TestZnsProxyKvmReconnectExtension();
        HostInventory host = kvmHost("host-uuid");
        host.setArchitecture(CpuArchitecture.aarch64.name());

        extension.connectionReestablished(host);

        assertEquals(0, extension.ensureCount);
    }

    @Test
    public void testReconnectSkipsRemoteInstallInUnitTestMode() throws Exception {
        boolean originalUnitTestOn = CoreGlobalProperty.UNIT_TEST_ON;
        try {
            CoreGlobalProperty.UNIT_TEST_ON = true;
            TestZnsProxyKvmReconnectExtension extension = new TestZnsProxyKvmReconnectExtension();

            extension.connectionReestablished(kvmHost("host-uuid"));

            assertEquals(0, extension.ensureCount);
        } finally {
            CoreGlobalProperty.UNIT_TEST_ON = originalUnitTestOn;
        }
    }

    @Test
    public void testConnectFlowEnsuresNewAddedHost() {
        TestZnsProxyKvmReconnectExtension extension = new TestZnsProxyKvmReconnectExtension();
        TestFlowTrigger trigger = new TestFlowTrigger();

        extension.createKvmHostConnectingFlow(connectContext("host-uuid", true)).run(trigger, new HashMap<>());

        assertEquals(1, extension.ensureCount);
        assertEquals("host-uuid", extension.lastEnsuredHostUuid);
        assertEquals(1, trigger.nextCount);
        assertEquals(0, trigger.failCount);
    }

    @Test
    public void testConnectFlowEnsuresReconnectedHost() {
        TestZnsProxyKvmReconnectExtension extension = new TestZnsProxyKvmReconnectExtension();
        TestFlowTrigger trigger = new TestFlowTrigger();

        Flow flow = extension.createKvmHostConnectingFlow(connectContext("host-uuid", false));
        flow.run(trigger, new HashMap<>());

        assertEquals(1, extension.ensureCount);
        assertEquals("host-uuid", extension.lastEnsuredHostUuid);
        assertEquals(1, trigger.nextCount);
        assertEquals(0, trigger.failCount);
    }

    @Test
    public void testConnectFlowSkipsUnsupportedHostArchitecture() {
        TestZnsProxyKvmReconnectExtension extension = new TestZnsProxyKvmReconnectExtension();
        TestFlowTrigger trigger = new TestFlowTrigger();
        KVMHostConnectedContext context = connectContext("host-uuid", false);
        context.getInventory().setArchitecture(CpuArchitecture.aarch64.name());

        extension.createKvmHostConnectingFlow(context).run(trigger, new HashMap<>());

        assertEquals(0, extension.ensureCount);
        assertEquals(1, trigger.nextCount);
        assertEquals(0, trigger.failCount);
    }

    @Test
    public void testConnectFlowPreservesPackageFailureDetails() {
        TestZnsProxyKvmReconnectExtension extension = new TestZnsProxyKvmReconnectExtension();
        TestFlowTrigger trigger = new TestFlowTrigger();
        extension.ensureFailure = new CloudRuntimeException("zns-proxy package checksum mismatch");

        extension.createKvmHostConnectingFlow(connectContext("host-uuid", false))
                .run(trigger, new HashMap<>());

        assertEquals(1, extension.ensureCount);
        assertEquals(0, trigger.nextCount);
        assertEquals(1, trigger.failCount);
        assertEquals("zns-proxy package checksum mismatch", trigger.error.getDetails());
    }

    @Test(expected = RuntimeException.class)
    public void testResolvePackageRejectsPathTraversal() throws Exception {
        File repo = tempFolder.newFolder("repo-traversal");
        ZnsProxyGlobalProperty.PACKAGE_REPOSITORY_PATH = repo.getAbsolutePath();
        ZnsProxyGlobalProperty.PROXY_PACKAGE_NAME = "../bad.bin";

        ZnsProxyInstaller.resolveAndVerifyPackage();
    }

    @Test(expected = RuntimeException.class)
    public void testResolvePackageFailsWhenMissing() throws Exception {
        File repo = tempFolder.newFolder("repo-missing");
        ZnsProxyGlobalProperty.PACKAGE_REPOSITORY_PATH = repo.getAbsolutePath();
        ZnsProxyGlobalProperty.PROXY_PACKAGE_NAME = "missing.bin";

        ZnsProxyInstaller.resolveAndVerifyPackage();
    }

    @Test
    public void testResolveAndVerifyPackageAcceptsValidManifest() throws Exception {
        File repo = tempFolder.newFolder("repo-valid-manifest");
        ZnsProxyGlobalProperty.PACKAGE_REPOSITORY_PATH = repo.getAbsolutePath();
        File pkg = new File(repo, "zns-proxy.bin");
        FileUtils.writeStringToFile(pkg, "proxy", StandardCharsets.UTF_8);
        writeManifest(repo, pkg, sha256(pkg), "1.2.0.1");

        File resolved = ZnsProxyInstaller.resolveAndVerifyPackage();

        assertEquals(pkg.getAbsolutePath(), resolved.getAbsolutePath());
    }

    @Test
    public void testResolveAndVerifyPackagePrefersCompleteClasspathReleaseOverStaleRepositoryPackage()
            throws Exception {
        File repo = tempFolder.newFolder("repo-stale-package");
        ZnsProxyGlobalProperty.PACKAGE_REPOSITORY_PATH = repo.getAbsolutePath();
        FileUtils.writeStringToFile(
                new File(repo, "zns-proxy.bin"), "stale-proxy", StandardCharsets.UTF_8);

        URL classPathRoot = getClass().getClassLoader().getResource("");
        File module = new File(new File(classPathRoot.toURI()),
                "ansible/znsproxy-" + System.nanoTime());
        assertTrue(module.mkdirs());
        ZnsProxyGlobalProperty.ANSIBLE_MODULE_PATH =
                "ansible/" + module.getName();
        File classPathPackage = new File(module, "zns-proxy.bin");
        FileUtils.writeStringToFile(classPathPackage, "current-proxy", StandardCharsets.UTF_8);
        writeManifest(module, classPathPackage, sha256(classPathPackage), "1.2.0.1");

        try {
            File resolved = ZnsProxyInstaller.resolveAndVerifyPackage();
            assertEquals(classPathPackage.getAbsolutePath(), resolved.getAbsolutePath());
        } finally {
            FileUtils.deleteDirectory(module);
        }
    }

    @Test(expected = RuntimeException.class)
    public void testResolveAndVerifyPackageRejectsMissingManifest() throws Exception {
        File repo = tempFolder.newFolder("repo-missing-manifest");
        ZnsProxyGlobalProperty.PACKAGE_REPOSITORY_PATH = repo.getAbsolutePath();
        FileUtils.writeStringToFile(new File(repo, "zns-proxy.bin"), "proxy", StandardCharsets.UTF_8);

        ZnsProxyInstaller.resolveAndVerifyPackage();
    }

    @Test(expected = RuntimeException.class)
    public void testResolveAndVerifyPackageRejectsShaMismatch() throws Exception {
        File repo = tempFolder.newFolder("repo-sha-mismatch");
        ZnsProxyGlobalProperty.PACKAGE_REPOSITORY_PATH = repo.getAbsolutePath();
        File pkg = new File(repo, "zns-proxy.bin");
        FileUtils.writeStringToFile(pkg, "proxy", StandardCharsets.UTF_8);
        writeManifest(repo, pkg,
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                "1.2.0.1");

        ZnsProxyInstaller.resolveAndVerifyPackage();
    }

    @Test(expected = RuntimeException.class)
    public void testResolveAndVerifyPackageRejectsNonCanonicalVersion() throws Exception {
        File repo = tempFolder.newFolder("repo-version-invalid");
        ZnsProxyGlobalProperty.PACKAGE_REPOSITORY_PATH = repo.getAbsolutePath();
        File pkg = new File(repo, "zns-proxy.bin");
        FileUtils.writeStringToFile(pkg, "proxy", StandardCharsets.UTF_8);
        writeManifest(repo, pkg, sha256(pkg), "1.2.0");

        ZnsProxyInstaller.resolveAndVerifyPackage();
    }

    private static HostInventory kvmHost(String uuid) {
        HostInventory host = new HostInventory();
        host.setUuid(uuid);
        host.setHypervisorType(KVMConstant.KVM_HYPERVISOR_TYPE);
        host.setArchitecture(CpuArchitecture.x86_64.name());
        return host;
    }

    private static KVMHostConnectedContext connectContext(String hostUuid, boolean newAddedHost) {
        KVMHostInventory host = new KVMHostInventory();
        host.setUuid(hostUuid);
        host.setHypervisorType(KVMConstant.KVM_HYPERVISOR_TYPE);
        host.setArchitecture(CpuArchitecture.x86_64.name());

        KVMHostConnectedContext context = new KVMHostConnectedContext();
        context.setInventory(host);
        context.setNewAddedHost(newAddedHost);
        return context;
    }

    private static class TestFlowTrigger implements FlowTrigger {
        private int nextCount;
        private int failCount;
        private ErrorCode error;

        @Override
        public void fail(ErrorCode errorCode) {
            failCount++;
            error = errorCode;
        }

        @Override
        public void next() {
            nextCount++;
        }

        @Override
        public void setError(ErrorCode error) {
            this.error = error;
        }
    }

    private static void writeManifest(File repo, File pkg, String sha256, String version) throws Exception {
        HashMap<String, Object> manifest = new HashMap<>();
        manifest.put("component", "zns-proxy");
        manifest.put("packageName", pkg.getName());
        manifest.put("version", version);
        manifest.put("arch", Collections.singletonList("amd64"));
        manifest.put("sha256", sha256);
        manifest.put("path", pkg.getName());
        manifest.put("buildTime", "2026-07-31T00:00:00Z");
        FileUtils.writeStringToFile(
                new File(repo, "zns-proxy-manifest.json"),
                new Gson().toJson(manifest),
                StandardCharsets.UTF_8);
    }

    private static String sha256(File file) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(FileUtils.readFileToByteArray(file));
        StringBuilder result = new StringBuilder();
        for (byte value : digest) {
            result.append(String.format("%02x", value & 0xff));
        }
        return result.toString();
    }

    private static class TestZnsProxyKvmReconnectExtension extends ZnsProxyKvmReconnectExtension {
        private int ensureCount;
        private String lastEnsuredHostUuid;
        private CloudRuntimeException ensureFailure;

        @Override
        protected void ensureHost(String hostUuid) {
            ensureCount++;
            lastEnsuredHostUuid = hostUuid;
            if (ensureFailure != null) {
                throw ensureFailure;
            }
        }

        @Override
        protected ErrorCode toOperationError(String details) {
            return new ErrorCode("SYS.1006", "Operation Error", details);
        }
    }
}
