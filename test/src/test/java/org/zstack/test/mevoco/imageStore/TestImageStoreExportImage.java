package org.zstack.test.mevoco.imageStore;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.MessageCommandRecorder;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageVO;
import org.zstack.header.storage.backup.APIExportImageFromBackupStorageMsg;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class TestImageStoreExportImage {
    private CLogger logger = Utils.getLogger(TestImageStoreExportImage.class);
    private Deployer deployer;
    private Api api;
    private DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestImageStoreCreateVmOnKvm.xml", con);
        deployer.addSpringConfig("mevocoRelated.xml");
        deployer.addSpringConfig("imagestore.xml");
        deployer.addSpringConfig("ImageStoreBackupStorageSimulator.xml");
        deployer.addSpringConfig("ImageStorePrimaryStorageSimulator.xml");
        deployer.build();
        api = deployer.getApi();
        ComponentLoader loader = deployer.getComponentLoader();
        loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        loader.getComponent(ImageStoreBackupStorageSimulatorConfig.class);
        api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        try {
            MessageCommandRecorder.reset();
            MessageCommandRecorder.start(APIExportImageFromBackupStorageMsg.class);

            BackupStorageInventory bs = deployer.backupStorages.get("imagestore");
            ImageInventory img = deployer.images.get("TestImage");
            api.exportImage(bs.getUuid(), img.getUuid());

            String callingChain = MessageCommandRecorder.endAndToString();
            logger.debug(callingChain);

            ImageVO imageVO = dbf.findByUuid(img.getUuid(), ImageVO.class);
            Assert.assertTrue(imageVO.getExportUrl() != null);

            api.delExportedImage(bs.getUuid(), img.getUuid());
            callingChain = MessageCommandRecorder.endAndToString();
            logger.debug(callingChain);

            imageVO = dbf.findByUuid(img.getUuid(), ImageVO.class);
            Assert.assertTrue(imageVO.getExportUrl() == null);

        } catch (ApiSenderException e) {
            Assert.fail(e.toString());
        }
    }
}
