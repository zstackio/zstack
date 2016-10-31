package org.zstack.test.kvm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.simulator.storage.backup.sftp.SftpBackupStorageSimulatorConfig;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * 1 create a vm from a data volume template
 * <p>
 * confirm the vm failed to create
 */
public class TestCreateVmOnKvm1 {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDeleteImage2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    SftpBackupStorageSimulatorConfig config;

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
        config = loader.getComponent(SftpBackupStorageSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        DiskOfferingInventory disk = new DiskOfferingInventory();
        disk.setName("xxx");
        disk.setDiskSize(10000000);
        disk = api.addDiskOffering(disk);
        api.stopVmInstance(vm.getUuid());
        VolumeInventory vol = api.createDataVolume("vol", disk.getUuid());
        vol = api.attachVolumeToVm(vm.getUuid(), vol.getUuid());
        ImageInventory img = api.addDataVolumeTemplateFromDataVolume(vol.getUuid(), null);

        InstanceOfferingInventory ioinv = deployer.instanceOfferings.get("TestInstanceOffering");

        VmCreator creator = new VmCreator(api);
        creator.addL3Network(vm.getVmNics().get(0).getL3NetworkUuid());
        creator.name = "vm";
        creator.imageUuid = img.getUuid();
        creator.instanceOfferingUuid = ioinv.getUuid();
        boolean s = false;
        try {
            creator.create();
        } catch (ApiSenderException e) {
            s = true;
        }

        Assert.assertTrue(s);
    }

}
