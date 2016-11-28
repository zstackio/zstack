package org.zstack.test.storage.backup.sftp;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.search.APISearchMessage.NOVTriple;
import org.zstack.header.search.SearchOp;
import org.zstack.simulator.storage.backup.sftp.SftpBackupStorageSimulatorConfig;
import org.zstack.storage.backup.sftp.APIGetSftpBackupStorageMsg;
import org.zstack.storage.backup.sftp.APISearchSftpBackupStorageMsg;
import org.zstack.storage.backup.sftp.SftpBackupStorageInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TestSearchSftpBackupStorage {
    CLogger logger = Utils.getLogger(TestSearchSftpBackupStorage.class);
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
        deployer = new Deployer("deployerXml/sftpBackupStorage/TestSearchSftpBackupStorage.xml", con);
        deployer.addSpringConfig("SftpBackupStorage.xml");
        deployer.addSpringConfig("SftpBackupStorageSimulator.xml");
        deployer.addSpringConfig("SearchManager.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(SftpBackupStorageSimulatorConfig.class);
        gcf = loader.getComponent(GlobalConfigFacade.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        TimeUnit.SECONDS.sleep(1);
        APISearchSftpBackupStorageMsg msg = new APISearchSftpBackupStorageMsg();
        NOVTriple t = new NOVTriple();
        t.setName("name");
        t.setOp(SearchOp.OR_EQ.toString());
        t.setVal("sftp1");
        msg.getNameOpValueTriples().add(t);

        String res = api.search(msg);
        List<SftpBackupStorageInventory> invs = JSONObjectUtil.toCollection(res, ArrayList.class, SftpBackupStorageInventory.class);
        Assert.assertEquals(1, invs.size());

        SftpBackupStorageInventory inv0 = invs.get(0);
        APIGetSftpBackupStorageMsg gmsg = new APIGetSftpBackupStorageMsg();
        gmsg.setUuid(inv0.getUuid());
        res = api.getInventory(gmsg);
        SftpBackupStorageInventory sinv = JSONObjectUtil.toObject(res, SftpBackupStorageInventory.class);
        Assert.assertEquals(inv0.getUrl(), sinv.getUrl());
    }
}
