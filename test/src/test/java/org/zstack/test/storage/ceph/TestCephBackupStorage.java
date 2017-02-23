package org.zstack.test.storage.ceph;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageDeletionPolicyManager.ImageDeletionPolicy;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.backup.APIAddBackupStorageEvent;
import org.zstack.image.ImageGlobalConfig;
import org.zstack.storage.ceph.backup.APIAddCephBackupStorageMsg;
import org.zstack.storage.ceph.backup.CephBackupStorageMonVO;
import org.zstack.storage.ceph.backup.CephBackupStorageSimulatorConfig;
import org.zstack.storage.ceph.backup.CephBackupStorageVO;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

import static org.zstack.utils.CollectionDSL.list;

/**
 * 1. add a ceph backup storage
 * <p>
 * confirm the backup storage added successfully
 * <p>
 * 2. add an image
 * <p>
 * confirm the image added successfully
 * <p>
 * 3. delete the image
 * <p>
 * confirm the image deleted successfully
 * 4.add a ceph backup storage
 * <p>
 * require the poolName when importImages is true
 */
public class TestCephBackupStorage {
    CLogger logger = Utils.getLogger(TestCephBackupStorage.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    GlobalConfigFacade gcf;
    SessionInventory session;
    CephBackupStorageSimulatorConfig config;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/ceph/TestCephBackupStorage.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("ceph.xml");
        deployer.addSpringConfig("cephSimulator.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        gcf = loader.getComponent(GlobalConfigFacade.class);
        config = loader.getComponent(CephBackupStorageSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        ImageGlobalConfig.DELETION_POLICY.updateValue(ImageDeletionPolicy.Direct.toString());
        CephBackupStorageVO bs = dbf.listAll(CephBackupStorageVO.class).get(0);
        Assert.assertEquals("7ff218d9-f525-435f-8a40-3618d1772a64", bs.getFsid());
        Assert.assertEquals(SizeUnit.TERABYTE.toByte(1), bs.getTotalCapacity());
        Assert.assertEquals(SizeUnit.GIGABYTE.toByte(500), bs.getAvailableCapacity());

        Assert.assertEquals(2, dbf.count(CephBackupStorageMonVO.class));
        Assert.assertFalse(config.downloadCmds.isEmpty());

        ImageInventory img = deployer.images.get("TestImage");
        api.deleteImage(img.getUuid());
        Assert.assertFalse(config.deleteCmds.isEmpty());

        APIAddCephBackupStorageMsg bmsg = new APIAddCephBackupStorageMsg();
        bmsg.setMonUrls(list("root:password@127.0.0.1:2222/?monPort=1234", "root1:password1@localhost:3322/?monPort=5678"));
        bmsg.setSession(api.getAdminSession());
        bmsg.setName("ceph-bs");
        bmsg.setImportImages(true);
        ApiSender sender = api.getApiSender();
        boolean s = false;
        try {
            sender.send(bmsg, APIAddBackupStorageEvent.class);
        } catch (ApiSenderException e) {
            s=true;
        }
        Assert.assertEquals(true,s);
    }
}
