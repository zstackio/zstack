package org.zstack.test.kvm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.storage.primary.nfs.NfsPrimaryStorageSimulatorConfig;
import org.zstack.storage.primary.nfs.NfsPrimaryStorageGlobalProperty;
import org.zstack.storage.primary.nfs.NfsPrimaryStorageKVMBackendCommands.DeleteCmd;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

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
 * confirm the volume and snapshot deleted
 */
public class TestDestroyVmOnKvm1 {
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
        final VolumeInventory root = vm.getRootVolume();

        final VolumeSnapshotInventory sp = api.createSnapshot(root.getUuid());

        HostInventory host = deployer.hosts.get("host1");
        api.stopVmInstance(vm.getUuid());

        HostVO hvo = dbf.findByUuid(host.getUuid(), HostVO.class);
        hvo.setStatus(HostStatus.Disconnected);
        dbf.update(hvo);

        api.destroyVmInstance(vm.getUuid());

        Assert.assertEquals(0, nconfig.deleteCmds.size());
        hvo.setStatus(HostStatus.Connected);
        dbf.update(hvo);

        TimeUnit.SECONDS.sleep(2);

        Assert.assertEquals(2, nconfig.deleteCmds.size());
        DeleteCmd cmd = CollectionUtils.find(nconfig.deleteCmds, new Function<DeleteCmd, DeleteCmd>() {
            @Override
            public DeleteCmd call(DeleteCmd arg) {
                return root.getInstallPath().equals(arg.getInstallPath()) ? arg : null;
            }
        });
        Assert.assertNotNull(cmd);

        cmd = CollectionUtils.find(nconfig.deleteCmds, new Function<DeleteCmd, DeleteCmd>() {
            @Override
            public DeleteCmd call(DeleteCmd arg) {
                return sp.getPrimaryStorageInstallPath().equals(arg.getInstallPath()) ? arg : null;
            }
        });
        Assert.assertNotNull(cmd);
    }
}
