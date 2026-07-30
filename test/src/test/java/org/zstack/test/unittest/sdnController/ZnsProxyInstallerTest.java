package org.zstack.test.unittest.sdnController;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostInventory;
import org.zstack.kvm.KVMConstant;
import org.zstack.kvm.KVMHostConnectedContext;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.sdnController.SdnControllerSystemTags;
import org.zstack.sdnController.ZnsProxyGlobalProperty;
import org.zstack.sdnController.znsproxy.ZnsProxyInstaller;
import org.zstack.sdnController.znsproxy.ZnsProxyKvmReconnectExtension;
import org.zstack.sdnController.znsproxy.ZnsProxyPrepareServiceCmd;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ZnsProxyInstallerTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private String originalPackageRepositoryPath;
    private String originalProxyPackageName;

    @Before
    public void setUp() {
        originalPackageRepositoryPath = ZnsProxyGlobalProperty.PACKAGE_REPOSITORY_PATH;
        originalProxyPackageName = ZnsProxyGlobalProperty.PROXY_PACKAGE_NAME;
    }

    @After
    public void tearDown() {
        ZnsProxyGlobalProperty.PACKAGE_REPOSITORY_PATH = originalPackageRepositoryPath;
        ZnsProxyGlobalProperty.PROXY_PACKAGE_NAME = originalProxyPackageName;
    }

    @Test
    public void testBuildInstallCommandUsesPackageDefaults() {
        String command = ZnsProxyInstaller.buildInstallCommand("/var/lib/zstack/zns-proxy/package/zns-proxy.bin");
        assertEquals("'/var/lib/zstack/zns-proxy/package/zns-proxy.bin' install", command);
    }

    @Test
    public void testResolvePackageByPackageName() throws Exception {
        File repo = tempFolder.newFolder("repo");
        ZnsProxyGlobalProperty.PACKAGE_REPOSITORY_PATH = repo.getAbsolutePath();
        File pkg = new File(repo, "zns-proxy-1.2.3.bin");
        FileUtils.writeStringToFile(pkg, "proxy", StandardCharsets.UTF_8);

        ZnsProxyPrepareServiceCmd cmd = new ZnsProxyPrepareServiceCmd();
        cmd.packageName = "zns-proxy-1.2.3.bin";

        assertEquals(pkg.getAbsolutePath(), ZnsProxyInstaller.resolvePackage(cmd).getAbsolutePath());
    }

    @Test
    public void testResolvePackageByProxyVersion() throws Exception {
        File repo = tempFolder.newFolder("repo-version");
        ZnsProxyGlobalProperty.PACKAGE_REPOSITORY_PATH = repo.getAbsolutePath();
        File pkg = new File(repo, "zns-proxy-1.2.3.bin");
        FileUtils.writeStringToFile(pkg, "proxy", StandardCharsets.UTF_8);

        ZnsProxyPrepareServiceCmd cmd = new ZnsProxyPrepareServiceCmd();
        cmd.proxyVersion = "1.2.3";

        assertEquals(pkg.getAbsolutePath(), ZnsProxyInstaller.resolvePackage(cmd).getAbsolutePath());
    }

    @Test
    public void testResolveDefaultPackageUsesProxyPackageName() throws Exception {
        File repo = tempFolder.newFolder("repo-default");
        ZnsProxyGlobalProperty.PACKAGE_REPOSITORY_PATH = repo.getAbsolutePath();
        ZnsProxyGlobalProperty.PROXY_PACKAGE_NAME = "zns-proxy.bin";
        File pkg = new File(repo, "zns-proxy.bin");
        FileUtils.writeStringToFile(pkg, "proxy", StandardCharsets.UTF_8);

        assertEquals(pkg.getAbsolutePath(), ZnsProxyInstaller.resolveDefaultPackage().getAbsolutePath());
    }

    @Test
    public void testZnsProxyPreparedTagTargetsHost() {
        assertEquals("znsProxy::prepared", SdnControllerSystemTags.ZNS_PROXY_PREPARED.getTagFormat());
        assertEquals(HostVO.class, SdnControllerSystemTags.ZNS_PROXY_PREPARED.getResourceClass());
    }

    @Test
    public void testPrepareServiceCmdAcceptsHostUuidsField() {
        ZnsProxyPrepareServiceCmd cmd = new Gson().fromJson(
                "{\"computeManagerUuid\":\"cm-uuid\",\"hostUuids\":[\"host-uuid-1\"]}",
                ZnsProxyPrepareServiceCmd.class);

        assertEquals(Arrays.asList("host-uuid-1"), cmd.hostUuids);
    }

    @Test
    public void testPrepareServiceCmdIgnoresLegacyHostUuidField() {
        ZnsProxyPrepareServiceCmd cmd = new Gson().fromJson(
                "{\"computeManagerUuid\":\"cm-uuid\",\"hostUuid\":[\"host-uuid-1\"]}",
                ZnsProxyPrepareServiceCmd.class);

        assertNull(cmd.hostUuids);
    }

    @Test
    public void testReconnectSkipsHostWithoutPreparedTag() throws Exception {
        TestZnsProxyKvmReconnectExtension extension = new TestZnsProxyKvmReconnectExtension(false);

        extension.connectionReestablished(kvmHost("host-uuid"));

        assertEquals(0, extension.reinstallCount);
    }

    @Test
    public void testReconnectReinstallsPreparedHost() throws Exception {
        TestZnsProxyKvmReconnectExtension extension = new TestZnsProxyKvmReconnectExtension(true);

        extension.connectionReestablished(kvmHost("host-uuid"));

        assertEquals(1, extension.reinstallCount);
        assertEquals("host-uuid", extension.lastReinstalledHostUuid);
    }

    @Test
    public void testReconnectSkipsNonKvmHost() throws Exception {
        TestZnsProxyKvmReconnectExtension extension = new TestZnsProxyKvmReconnectExtension(true);
        HostInventory host = kvmHost("host-uuid");
        host.setHypervisorType("Simulator");

        extension.connectionReestablished(host);

        assertEquals(0, extension.reinstallCount);
    }

    @Test
    public void testConnectFlowSkipsNewAddedHost() {
        TestZnsProxyKvmReconnectExtension extension = new TestZnsProxyKvmReconnectExtension(true);
        TestFlowTrigger trigger = new TestFlowTrigger();

        extension.createKvmHostConnectingFlow(connectContext("host-uuid", true)).run(trigger, new HashMap<>());

        assertEquals(0, extension.reinstallCount);
        assertEquals(1, trigger.nextCount);
        assertEquals(0, trigger.failCount);
    }

    @Test
    public void testConnectFlowReinstallsPreparedReconnectedHost() {
        TestZnsProxyKvmReconnectExtension extension = new TestZnsProxyKvmReconnectExtension(true);
        TestFlowTrigger trigger = new TestFlowTrigger();

        Flow flow = extension.createKvmHostConnectingFlow(connectContext("host-uuid", false));
        flow.run(trigger, new HashMap<>());

        assertEquals(1, extension.reinstallCount);
        assertEquals("host-uuid", extension.lastReinstalledHostUuid);
        assertEquals(1, trigger.nextCount);
        assertEquals(0, trigger.failCount);
    }

    @Test(expected = RuntimeException.class)
    public void testResolvePackageRejectsPathTraversal() throws Exception {
        File repo = tempFolder.newFolder("repo-traversal");
        ZnsProxyGlobalProperty.PACKAGE_REPOSITORY_PATH = repo.getAbsolutePath();

        ZnsProxyPrepareServiceCmd cmd = new ZnsProxyPrepareServiceCmd();
        cmd.packageName = "../bad.bin";

        ZnsProxyInstaller.resolvePackage(cmd);
    }

    @Test(expected = RuntimeException.class)
    public void testResolvePackageFailsWhenMissing() throws Exception {
        File repo = tempFolder.newFolder("repo-missing");
        ZnsProxyGlobalProperty.PACKAGE_REPOSITORY_PATH = repo.getAbsolutePath();

        ZnsProxyPrepareServiceCmd cmd = new ZnsProxyPrepareServiceCmd();
        cmd.packageName = "missing.bin";

        ZnsProxyInstaller.resolvePackage(cmd);
    }

    private static HostInventory kvmHost(String uuid) {
        HostInventory host = new HostInventory();
        host.setUuid(uuid);
        host.setHypervisorType(KVMConstant.KVM_HYPERVISOR_TYPE);
        return host;
    }

    private static KVMHostConnectedContext connectContext(String hostUuid, boolean newAddedHost) {
        KVMHostInventory host = new KVMHostInventory();
        host.setUuid(hostUuid);
        host.setHypervisorType(KVMConstant.KVM_HYPERVISOR_TYPE);

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

    private static class TestZnsProxyKvmReconnectExtension extends ZnsProxyKvmReconnectExtension {
        private final boolean prepared;
        private int reinstallCount;
        private String lastReinstalledHostUuid;

        private TestZnsProxyKvmReconnectExtension(boolean prepared) {
            this.prepared = prepared;
        }

        @Override
        protected boolean isZnsProxyPrepared(String hostUuid) {
            return prepared;
        }

        @Override
        protected void reinstallPreparedHost(String hostUuid) {
            reinstallCount++;
            lastReinstalledHostUuid = hostUuid;
        }
    }
}
