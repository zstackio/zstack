package org.zstack.test.storage.primary.iscsi;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.KVMAgentCommands.AttachDataVolumeCmd;
import org.zstack.kvm.KVMAgentCommands.VolumeTO;
import org.zstack.kvm.KVMConstant;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.primary.iscsi.IscsiBtrfsPrimaryStorageSimulatorConfig;
import org.zstack.storage.primary.iscsi.IscsiVolumePath;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

import java.util.List;

/**
 * 1. create VM with IscsiBtrfsPrimaryStorage
 * 2. attach a data volume
 * 3. detach the data volume
 * 4. create a template from the data volume
 * 5. create a data volume from the template
 *
 * confirm the volume is created of format raw
 *
 */
public class TestIscsiBtrfsPrimaryStorage5 {
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
        AttachDataVolumeCmd cmd = kconfig.attachDataVolumeCmds.get(0);
        Assert.assertNotNull(cmd);
        Assert.assertEquals(VolumeTO.ISCSI, cmd.getVolume().getDeviceType());
        Assert.assertEquals(vol.getUuid(), cmd.getVolume().getVolumeUuid());

        api.detachVolumeFromVm(vol.getUuid());
        ImageInventory img = api.addDataVolumeTemplateFromDataVolume(vol.getUuid(), (List) null);
        Assert.assertEquals(VolumeConstant.VOLUME_FORMAT_RAW, img.getFormat());
        Assert.assertEquals(1, iconfig.uploadToSftpCmds.size());

        PrimaryStorageInventory ps = deployer.primaryStorages.get("TestPrimaryStorage");
        VolumeInventory dv = api.createDataVolumeFromTemplate(img.getUuid(), ps.getUuid());
        Assert.assertEquals(VolumeConstant.VOLUME_FORMAT_RAW, dv.getFormat());
        Assert.assertTrue(dv.getInstallPath().contains("iscsi://") && dv.getInstallPath().contains("file://"));
        Assert.assertEquals(1, iconfig.createIscsiTargetCmds.size());
    }
}
