package org.zstack.test.storage.backup.sftp;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageVO;
import org.zstack.header.simulator.SimulatorConstant;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.simulator.storage.backup.sftp.SftpBackupStorageSimulatorConfig;
import org.zstack.storage.backup.sftp.SftpBackupStorageInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class TestSftpBackupStorageDownloadImageFailure2 {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDownloadImageFailure2.class);
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
        config.downloadSuccess2 = false;
        ImageInventory iinv = new ImageInventory();
        iinv.setMediaType(ImageMediaType.RootVolumeTemplate.toString());
        iinv.setFormat(VolumeConstant.VOLUME_FORMAT_QCOW2);
        iinv.setFormat(SimulatorConstant.SIMULATOR_VOLUME_FORMAT_STRING);
        iinv.setGuestOsType("CentOS6.3");
        iinv.setName("TestImage");
        iinv.setType(ImageConstant.ZSTACK_IMAGE_TYPE);
        iinv.setUrl("http://zstack.org/download/testimage.qcow2");
        try {
            api.addImage(iinv, sinv.getUuid());
        } catch (ApiSenderException e) {
        }
        long count = dbf.count(ImageVO.class);
        Assert.assertEquals(0, count);
    }
}