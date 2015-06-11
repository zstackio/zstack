package org.zstack.test.storage.primary.iscsi;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.KVMAgentCommands.AttachDataVolumeCmd;
import org.zstack.kvm.KVMAgentCommands.DetachDataVolumeCmd;
import org.zstack.kvm.KVMAgentCommands.VolumeTO;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.primary.iscsi.IscsiBtrfsPrimaryStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

/**
 * 1. create VM with IscsiBtrfsPrimaryStorage
 * 2. attach a data volume
 * 3. detach the data volume
 * 4. re-attach the data volume
 *
 * confirm the data volume is detached as ISCSI type
 * confirm the iscsi target is created when re-attaching the volume
 *
 */
public class TestIscsiBtrfsPrimaryStorage2 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    KVMSimulatorConfig kconfig;
    IscsiBtrfsPrimaryStorageSimulatorConfig iconfig;

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
        iconfig = loader.getComponent(IscsiBtrfsPrimaryStorageSimulatorConfig.class);
    }
    
    @Test
    public void test() throws ApiSenderException, InterruptedException {
        DiskOfferingInventory disk = deployer.diskOfferings.get("TestDataDiskOffering");
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VolumeInventory vol = api.createDataVolume("data", disk.getUuid());
        api.attachVolumeToVm(vm.getUuid(), vol.getUuid());
        // no instantiated volume doesn't need to create target separately
        Assert.assertEquals(0, iconfig.createIscsiTargetCmds.size());

        api.detachVolumeFromVm(vol.getUuid());
        Assert.assertEquals(1, iconfig.deleteIscsiTargetCmds.size());
        DetachDataVolumeCmd cmd = kconfig.detachDataVolumeCmds.get(0);
        Assert.assertNotNull(cmd);
        Assert.assertEquals(VolumeTO.ISCSI, cmd.getVolume().getDeviceType());
        Assert.assertEquals(vol.getUuid(), cmd.getVolume().getVolumeUuid());

        api.attachVolumeToVm(vm.getUuid(), vol.getUuid());
        Assert.assertEquals(1, iconfig.createIscsiTargetCmds.size());
    }
}
