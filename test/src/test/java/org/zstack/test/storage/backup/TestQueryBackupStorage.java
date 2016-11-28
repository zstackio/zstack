package org.zstack.test.storage.backup;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.query.QueryOp;
import org.zstack.header.storage.backup.APIQueryBackupStorageMsg;
import org.zstack.header.storage.backup.APIQueryBackupStorageReply;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.search.QueryTestValidator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class TestQueryBackupStorage {
    CLogger logger = Utils.getLogger(TestQueryBackupStorage.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/backupStorage/TestQueryBackupStorage.xml", con);
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        BackupStorageInventory bsinv = deployer.backupStorages.get("backup1");
        QueryTestValidator.validateEQ(new APIQueryBackupStorageMsg(), api, APIQueryBackupStorageReply.class, bsinv);
        QueryTestValidator.validateRandomEQConjunction(new APIQueryBackupStorageMsg(), api, APIQueryBackupStorageReply.class, bsinv, 2);

        ZoneInventory zone2 = deployer.zones.get("Zone2");
        APIQueryBackupStorageMsg msg = new APIQueryBackupStorageMsg();
        msg.addQueryCondition("attachedZoneUuids", QueryOp.EQ, zone2.getUuid());
        APIQueryBackupStorageReply reply = api.query(msg, APIQueryBackupStorageReply.class);
        Assert.assertEquals(2, reply.getInventories().size());

        ZoneInventory zone1 = deployer.zones.get("Zone1");
        msg = new APIQueryBackupStorageMsg();
        msg.addQueryCondition("attachedZoneUuids", QueryOp.NOT_IN, zone1.getUuid());
        reply = api.query(msg, APIQueryBackupStorageReply.class);
        Assert.assertEquals(2, reply.getInventories().size());

        msg = new APIQueryBackupStorageMsg();
        msg.addQueryCondition("attachedZoneUuids", QueryOp.IN, zone2.getUuid(), zone1.getUuid());
        reply = api.query(msg, APIQueryBackupStorageReply.class);
        Assert.assertEquals(2, reply.getInventories().size());
    }
}
