package org.zstack.test.storage.primary.iscsi;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.query.QueryOp;
import org.zstack.header.storage.primary.*;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.primary.iscsi.APIQueryIscsiFileSystemBackendPrimaryStorageMsg;
import org.zstack.storage.primary.iscsi.APIQueryIscsiFileSystemBackendPrimaryStorageReply;
import org.zstack.storage.primary.iscsi.IscsiFileSystemBackendPrimaryStorageInventory;
import org.zstack.storage.primary.iscsi.IscsiFileSystemBackendPrimaryStorageVO;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.search.QueryTestValidator;
import org.zstack.utils.gson.JSONObjectUtil;

public class TestQueryIscsiBtrfsPrimaryStorage {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    KVMSimulatorConfig kconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/iscsiBtrfsPrimaryStorage/TestIscsiBtrfsPrimaryStorage.xml", con);
        deployer.addSpringConfig("iscsiBtrfsPrimaryStorage.xml");
        deployer.addSpringConfig("iscsiFileSystemPrimaryStorageSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
    }
    
    @Test
    public void test() throws ApiSenderException, InterruptedException {
        IscsiFileSystemBackendPrimaryStorageVO vo = dbf.listAll(IscsiFileSystemBackendPrimaryStorageVO.class).get(0);
        IscsiFileSystemBackendPrimaryStorageInventory inv = IscsiFileSystemBackendPrimaryStorageInventory.valueOf(vo);
        QueryTestValidator.validateEQ(new APIQueryIscsiFileSystemBackendPrimaryStorageMsg(), api, APIQueryIscsiFileSystemBackendPrimaryStorageReply.class, inv);

        APIQueryPrimaryStorageMsg qmsg = new APIQueryPrimaryStorageMsg();
        qmsg.addQueryCondition("uuid", QueryOp.EQ, inv.getUuid());
        APIQueryPrimaryStorageReply r =  api.query(qmsg, APIQueryPrimaryStorageReply.class);
        PrimaryStorageInventory pinv = r.getInventories().get(0);
        Assert.assertTrue(pinv instanceof IscsiFileSystemBackendPrimaryStorageInventory);
        Assert.assertEquals(inv.getUuid(), pinv.getUuid());
    }
}
