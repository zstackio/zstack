package org.zstack.test.storage.backup.sftp;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageVO;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.simulator.storage.backup.sftp.SftpBackupStorageSimulatorConfig;
import org.zstack.storage.backup.sftp.SftpBackupStorageInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

public class TestSftpBackupStorageDownloadImage {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDownloadImage.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    GlobalConfigFacade gcf;
    SessionInventory session;
    SftpBackupStorageSimulatorConfig config;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/sftpBackupStorage/TestAddSftpBackupStorage.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        gcf = loader.getComponent(GlobalConfigFacade.class);
        config = loader.getComponent(SftpBackupStorageSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        SftpBackupStorageTestHelper helper = new SftpBackupStorageTestHelper();
        SftpBackupStorageInventory sinv = helper.addSimpleHttpBackupStorage(api);
        config.downloadSuccess1 = true;
        config.downloadSuccess2 = true;
        config.imageMd5sum = Platform.getUuid();
        long size = SizeUnit.GIGABYTE.toByte(8);
        ImageInventory iinv = new ImageInventory();
        iinv.setUuid(Platform.getUuid());
        iinv.setMediaType(ImageMediaType.RootVolumeTemplate.toString());
        iinv.setFormat(VolumeConstant.VOLUME_FORMAT_QCOW2);
        iinv.setGuestOsType("CentOS6.3");
        iinv.setName("TestImage");
        iinv.setType(ImageConstant.ZSTACK_IMAGE_TYPE);
        iinv.setUrl("http://zstack.org/download/testimage.qcow2");

        config.imageSizes.put(iinv.getUuid(), size);

        long asize = SizeUnit.GIGABYTE.toByte(1);
        config.imageActualSizes.put(iinv.getUuid(), asize);

        iinv = api.addImage(iinv, sinv.getUuid());
        Assert.assertEquals(size, iinv.getSize());
        Assert.assertEquals(asize, iinv.getActualSize().longValue());
        Assert.assertEquals(config.imageMd5sum, iinv.getMd5Sum());
        Assert.assertNotNull(iinv.getBackupStorageRefs().get(0).getInstallPath());
        ImageVO vo = dbf.findByUuid(iinv.getUuid(), ImageVO.class);
        Assert.assertEquals(size, vo.getSize());
        Assert.assertEquals(config.imageMd5sum, vo.getMd5Sum());
        Assert.assertNotNull(vo.getBackupStorageRefs().iterator().next().getInstallPath());
    }
}