package org.zstack.test.image;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageBackupStorageRefInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageStatus;
import org.zstack.header.image.ImageVO;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.simulator.storage.backup.sftp.SftpBackupStorageSimulatorConfig;
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
import java.util.List;

/**
 * 1. create image from root volume on two backup storage
 * <p>
 * confirm creation succeeds
 */
public class TestCreateTemplateFromRootVolume2 {
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
        deployer = new Deployer("deployerXml/image/TestCreateTemplateFromRootVolume1.xml", con);
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
        api.stopVmInstance(vm.getUuid());
        String rootVolumeUuid = vm.getRootVolumeUuid();
        VolumeVO vol = dbf.findByUuid(rootVolumeUuid, VolumeVO.class);

        BackupStorageInventory sftp = deployer.backupStorages.get("sftp");
        BackupStorageInventory sftp1 = deployer.backupStorages.get("sftp1");
        ImageInventory image = api.createTemplateFromRootVolume("testImage", rootVolumeUuid, Arrays.asList(sftp.getUuid(), sftp1.getUuid()));
        Assert.assertEquals(2, image.getBackupStorageRefs().size());
        Assert.assertEquals(ImageStatus.Ready.toString(), image.getStatus());
        Assert.assertEquals(vol.getSize(), image.getSize());
        Assert.assertEquals(String.format("volume://%s", vol.getUuid()), image.getUrl());

        List<String> bsUuids = CollectionUtils.transformToList(image.getBackupStorageRefs(), new Function<String, ImageBackupStorageRefInventory>() {
            @Override
            public String call(ImageBackupStorageRefInventory arg) {
                return arg.getBackupStorageUuid();
            }
        });

        Assert.assertTrue(bsUuids.containsAll(Arrays.asList(sftp.getUuid(), sftp1.getUuid())));

        ImageVO ivo = dbf.findByUuid(image.getUuid(), ImageVO.class);
        Assert.assertNotNull(ivo);
    }

}
