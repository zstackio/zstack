package org.zstack.test.storage.primary.iscsi;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.vm.*;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.kvm.KVMAgentCommands.StartVmCmd;
import org.zstack.kvm.KVMAgentCommands.VolumeTO;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.primary.iscsi.IscsiFileSystemBackendPrimaryStorageVO;
import org.zstack.storage.primary.iscsi.KVMIscsiVolumeTO;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

/**
 * 1. create VM with IscsiBtrfsPrimaryStorage
 *
 * confirm success
 */
public class TestIscsiBtrfsPrimaryStorage {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    KVMSimulatorConfig kconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/iscsiBtrfsPrimaryStorage/TestIscsiBtrfsPrimaryStorage.xml", con);
        deployer.addSpringConfig("iscsiBtrfsPrimaryStorage.xml");
        deployer.addSpringConfig("iscsiFileSystemPrimaryStorageSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
    }
    
    @Test
    public void test() throws ApiSenderException, InterruptedException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        for (VolumeInventory vol : vm.getAllVolumes()) {
            IscsiFileSystemBackendPrimaryStorageVO vo = dbf.findByUuid(vol.getPrimaryStorageUuid(), IscsiFileSystemBackendPrimaryStorageVO.class);
            Assert.assertNotNull(vo);
            Assert.assertTrue(vol.getInstallPath().startsWith("iscsi"));
        }

        StartVmCmd cmd = kconfig.startVmCmd;
        Assert.assertNotNull(cmd);
        for (VolumeTO to : cmd.getDataVolumes()) {
            Assert.assertTrue(VolumeTO.ISCSI.endsWith(to.getDeviceType()));
        }
        Assert.assertTrue(VolumeTO.ISCSI.endsWith(cmd.getRootVolume().getDeviceType()));
    }
}
