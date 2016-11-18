package org.zstack.test.kvm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.gc.GCStatus;
import org.zstack.core.gc.GarbageCollectorVO;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.storage.primary.nfs.NfsPrimaryStorageSimulatorConfig;
import org.zstack.storage.primary.nfs.NfsPrimaryStorageGlobalProperty;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 1. stop the vm
 * 2. make the host disconnected
 * 3. destroy the vm
 * <p>
 * confirm the vm destroyed successfully
 * <p>
 * 4. change the host to connected
 * <p>
 * confirm the vm is GCed
 */
public class TestDestroyVmOnKvm5 {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDeleteImage2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    KVMSimulatorConfig config;
    NfsPrimaryStorageSimulatorConfig nconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestCreateVmOnKvm.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(KVMSimulatorConfig.class);
        nconfig = loader.getComponent(NfsPrimaryStorageSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicy.Direct.toString());
        NfsPrimaryStorageGlobalProperty.BITS_DELETION_GC_INTERVAL = 1;
        VmInstanceInventory vm = deployer.vms.get("TestVm");

        HostInventory host = deployer.hosts.get("host1");

        HostVO hvo = dbf.findByUuid(host.getUuid(), HostVO.class);
        hvo.setStatus(HostStatus.Disconnected);
        dbf.update(hvo);

        api.destroyVmInstance(vm.getUuid());

        Assert.assertNull(config.destroyedVmUuid);

        api.reconnectHost(host.getUuid());

        TimeUnit.SECONDS.sleep(3);

        Assert.assertEquals(vm.getUuid(), config.destroyedVmUuid);

        List<GarbageCollectorVO> vos = dbf.listAll(GarbageCollectorVO.class);
        for (GarbageCollectorVO vo : vos) {
            org.junit.Assert.assertEquals(GCStatus.Done, vo.getStatus());
        }
    }
}
