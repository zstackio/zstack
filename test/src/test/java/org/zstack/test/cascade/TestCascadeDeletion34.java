package org.zstack.test.cascade;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterOfferingVO;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

/**
 * 1. have VR images on a backup storage
 * 2. have a virtual route offering referring to that image
 * 3. delete the backup storage
 * <p>
 * confirm the virtual router offering is deleted as well
 */
public class TestCascadeDeletion34 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/virtualRouter/virtualRouterSNAT.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("VirtualRouterSimulator.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        api.setTimeout(10000000);
        BackupStorageInventory bs = deployer.backupStorages.get("sftp");
        api.deleteBackupStorage(bs.getUuid());
        CascadeTestHelper helper = new CascadeTestHelper();
        helper.zeroInDatabase(VirtualRouterOfferingVO.class);
    }
}
