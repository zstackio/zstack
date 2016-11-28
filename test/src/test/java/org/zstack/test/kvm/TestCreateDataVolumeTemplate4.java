package org.zstack.test.kvm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageStatus;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeType;
import org.zstack.simulator.storage.primary.nfs.NfsPrimaryStorageSimulatorConfig;
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

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * 1. create a data volume template
 * 2. setup creating data volume to fail
 * 3. create data volume from the template
 * <p>
 * confirm volume failed to create, and primary storage capacity is returned
 */
public class TestCreateDataVolumeTemplate4 {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDeleteImage2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    NfsPrimaryStorageSimulatorConfig config;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestCreateDataVolumeTemplate.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(NfsPrimaryStorageSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        api.stopVmInstance(vm.getUuid());
        VolumeInventory dataVolume = CollectionUtils.find(vm.getAllVolumes(), new Function<VolumeInventory, VolumeInventory>() {
            @Override
            public VolumeInventory call(VolumeInventory arg) {
                return VolumeType.Data.toString().equals(arg.getType()) ? arg : null;
            }
        });

        BackupStorageInventory sftp = deployer.backupStorages.get("sftp");
        PrimaryStorageInventory nfs = deployer.primaryStorages.get("nfs");
        ImageInventory template = api.addDataVolumeTemplateFromDataVolume(dataVolume.getUuid(), Arrays.asList(sftp.getUuid()));
        Assert.assertEquals(ImageStatus.Ready.toString(), template.getStatus());
        PrimaryStorageVO psvo = dbf.findByUuid(nfs.getUuid(), PrimaryStorageVO.class);
        config.downloadFromSftpSuccess = false;
        boolean s = false;
        try {
            api.createDataVolumeFromTemplate(template.getUuid(), nfs.getUuid());
        } catch (ApiSenderException e) {
            TimeUnit.SECONDS.sleep(3);
            PrimaryStorageVO psvo1 = dbf.findByUuid(nfs.getUuid(), PrimaryStorageVO.class);
            Assert.assertEquals(psvo.getCapacity().getAvailableCapacity(), psvo1.getCapacity().getAvailableCapacity());
            s = true;
        }
        Assert.assertTrue(s);
    }
}
