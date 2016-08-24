package org.zstack.test.mevoco.imageStore;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.MessageCommandRecorder;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.primary.APICommitVolumeAsImageMsg;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Collections;
import java.util.List;

public class TestImageStoreCommitVolume {
    private CLogger logger = Utils.getLogger(TestImageStoreCommitVolume.class);
    private Deployer deployer;
    private Api api;

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
        loader.getComponent(DatabaseFacade.class);
        loader.getComponent(ImageStoreBackupStorageSimulatorConfig.class);
        api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");

        // Commit a volume as image
        try {
            MessageCommandRecorder.reset();
            MessageCommandRecorder.start(APICommitVolumeAsImageMsg.class);

            BackupStorageInventory bs = deployer.backupStorages.get("imagestore");
            List<String> bsUuids = Collections.singletonList(bs.getUuid());
            api.commitVolumeAsImage(
                    vm.getRootVolumeUuid(),
                    "test-image",
                    bsUuids
            );

            String callingChain = MessageCommandRecorder.endAndToString();
            logger.debug(callingChain);
        } catch (ApiSenderException e) {
            Assert.fail(e.getMessage());
        }
    }
}
