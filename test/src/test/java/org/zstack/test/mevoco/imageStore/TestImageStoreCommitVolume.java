package org.zstack.test.mevoco.imageStore;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.MessageCommandRecorder;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.image.APICreateRootVolumeTemplateFromRootVolumeMsg;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.volume.VolumeVO;
import org.zstack.image.ImageGlobalConfig;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.tag.TestQemuAgentSystemTag;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TestImageStoreCommitVolume {
    private CLogger logger = Utils.getLogger(TestImageStoreCommitVolume.class);
    private Deployer deployer;
    private DatabaseFacade dbf;
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
        dbf = loader.getComponent(DatabaseFacade.class);
        loader.getComponent(ImageStoreBackupStorageSimulatorConfig.class);
        api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        api.createSystemTag(vm.getUuid(), TestQemuAgentSystemTag.TestSystemTags.qemu.getTagFormat(), VmInstanceVO.class);
        Assert.assertEquals(TestQemuAgentSystemTag.TestSystemTags.qemu.getTagFormat(), getResourceUuidTag(vm.getUuid()));

        // Commit a volume as image
        MessageCommandRecorder.reset();
        MessageCommandRecorder.start(APICreateRootVolumeTemplateFromRootVolumeMsg.class);

        BackupStorageInventory bs = deployer.backupStorages.get("imagestore");
        List<String> bsUuids = Collections.singletonList(bs.getUuid());
        ImageInventory inv = api.createTemplateFromRootVolume(
                "test-image",
                vm.getRootVolumeUuid(),
                bsUuids
        );

        String callingChain = MessageCommandRecorder.endAndToString();
        logger.debug(callingChain);

        Assert.assertTrue(inv != null);

        // make sure the commited-image has the same SystemTags as vmInstance
        String tag = getResourceUuidTag(inv.getUuid());
        Assert.assertEquals(TestQemuAgentSystemTag.TestSystemTags.qemu.getTagFormat(), tag);

        // Expunge the root image of the VM and try to commit again
        final VolumeVO vol = dbf.findByUuid(vm.getRootVolumeUuid(), VolumeVO.class);
        Assert.assertTrue("Root volume is missing!", vol != null);

        ImageGlobalConfig.EXPUNGE_PERIOD.updateValue(1);
        ImageGlobalConfig.EXPUNGE_INTERVAL.updateValue(1);
        api.deleteImage(vol.getRootImageUuid(), bsUuids, null);
        TimeUnit.SECONDS.sleep(2);

        inv = api.createTemplateFromRootVolume(
                "test-image2",
                vm.getRootVolumeUuid(),
                bsUuids
        );
        Assert.assertTrue(inv != null);
    }

    String getResourceUuidTag(String resourceUuid) {
        SimpleQuery<SystemTagVO> pq = dbf.createQuery(SystemTagVO.class);
        pq.select(SystemTagVO_.tag);
        pq.add(SystemTagVO_.resourceUuid, SimpleQuery.Op.EQ, resourceUuid);
        String tag = pq.findValue();
        return tag;
    }
}
