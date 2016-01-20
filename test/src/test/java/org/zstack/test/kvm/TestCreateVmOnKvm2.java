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
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStorageStatus;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.storage.backup.sftp.SftpBackupStorageSimulatorConfig;
import org.zstack.simulator.storage.primary.nfs.NfsPrimaryStorageSimulatorConfig;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * 1. create a vm which will download the image in the primary storage cache
 * 2. make the backup storage disconnected
 * 3. create a new vm
 *
 * confirm the vm created successfully
 * confirm no download operation executed
 */
public class TestCreateVmOnKvm2 {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDeleteImage2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    SftpBackupStorageSimulatorConfig config;
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
        config = loader.getComponent(SftpBackupStorageSimulatorConfig.class);
        nconfig = loader.getComponent(NfsPrimaryStorageSimulatorConfig.class);
        session = api.loginAsAdmin();
    }
    
	@Test
	public void test() throws ApiSenderException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        BackupStorageInventory sftp = deployer.backupStorages.get("sftp");
        BackupStorageVO bsvo = dbf.findByUuid(sftp.getUuid(), BackupStorageVO.class);
        bsvo.setStatus(BackupStorageStatus.Disconnected);
        dbf.update(bsvo);

        InstanceOfferingInventory ioinv = deployer.instanceOfferings.get("TestInstanceOffering");
        ImageInventory img = deployer.images.get("TestImage");

        nconfig.downloadFromSftpCmds.clear();
        VmCreator creator = new VmCreator(api);
        creator.addL3Network(vm.getVmNics().get(0).getL3NetworkUuid());
        creator.name = "vm";
        creator.imageUuid = img.getUuid();
        creator.instanceOfferingUuid = ioinv.getUuid();
        creator.create();
        Assert.assertTrue(nconfig.downloadFromSftpCmds.isEmpty());
    }

}
