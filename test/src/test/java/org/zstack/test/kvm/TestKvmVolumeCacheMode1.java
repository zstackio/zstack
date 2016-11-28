package org.zstack.test.kvm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.KVMAgentCommands.AttachDataVolumeCmd;
import org.zstack.kvm.KVMAgentCommands.StartVmCmd;
import org.zstack.kvm.KVMAgentCommands.VolumeTO;
import org.zstack.kvm.KVMGlobalConfig;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class TestKvmVolumeCacheMode1 {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDeleteImage2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    KVMSimulatorConfig config;
    String mode = "writethrough";

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestKvmVolumeCacheMode1.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.load();

        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);

        KVMGlobalConfig.LIBVIRT_CACHE_MODE.updateValue(mode);

        deployer.build();
        api = deployer.getApi();
        config = loader.getComponent(KVMSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        StartVmCmd cmd = config.startVmCmd;
        for (VolumeTO to : cmd.getDataVolumes()) {
            Assert.assertEquals(mode, to.getCacheMode());
        }
        Assert.assertEquals(mode, cmd.getRootVolume().getCacheMode());

        DiskOfferingInventory dinv = deployer.diskOfferings.get("DiskOffering");
        VolumeInventory vol = api.createDataVolume("data", dinv.getUuid());
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        api.attachVolumeToVm(vm.getUuid(), vol.getUuid());
        Assert.assertEquals(1, config.attachDataVolumeCmds.size());
        AttachDataVolumeCmd acmd = config.attachDataVolumeCmds.get(0);
        Assert.assertEquals(mode, acmd.getVolume().getCacheMode());
    }
}
