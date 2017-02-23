package org.zstack.test.storage.ceph;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.backup.APIAddBackupStorageEvent;
import org.zstack.storage.ceph.backup.APIAddCephBackupStorageMsg;
import org.zstack.storage.ceph.backup.CephBackupStorageSimulatorConfig;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.utils.CollectionDSL.list;

/**
 * 1.add ceph backup storage
 * <p>
 * require poolName when importImages is true when create ceph BS
 */
public class TestCeph21 {

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
    public void test() throws Exception{
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