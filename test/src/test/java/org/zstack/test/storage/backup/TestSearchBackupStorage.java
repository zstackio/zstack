package org.zstack.test.storage.backup;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.search.APISearchMessage.NOLTriple;
import org.zstack.header.search.SearchOp;
import org.zstack.header.storage.backup.APIGetBackupStorageMsg;
import org.zstack.header.storage.backup.APISearchBackupStorageMsg;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.zone.ZoneVO;
import org.zstack.header.zone.ZoneVO_;
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

public class TestSearchBackupStorage {
    CLogger logger = Utils.getLogger(TestSearchBackupStorage.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/backupStorage/TestSearchBackupStorage.xml", con);
        deployer.addSpringConfig("SearchManager.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        TimeUnit.SECONDS.sleep(2);

        SimpleQuery<ZoneVO> zq = dbf.createQuery(ZoneVO.class);
        zq.add(ZoneVO_.name, Op.EQ, "Zone2");
        ZoneVO zone = zq.find();
        List<String> zone2Uuid = new ArrayList<String>();
        zone2Uuid.add(zone.getUuid());

        APISearchBackupStorageMsg msg = new APISearchBackupStorageMsg();
        NOLTriple tl = new NOLTriple();
        tl.setName("attachedZoneUuids");
        tl.setVals(zone2Uuid);
        tl.setOp(SearchOp.OR_NOT_IN.toString());
        msg.getNameOpListTriples().add(tl);

        String content = api.search(msg);
        List<BackupStorageInventory> invs = JSONObjectUtil.toCollection(content, ArrayList.class, BackupStorageInventory.class);
        Assert.assertEquals(1, invs.size());
        Assert.assertEquals("backup2", invs.get(0).getName());
        BackupStorageInventory backup2 = invs.get(0);

        api.detachBackupStorage(backup2.getUuid(), backup2.getAttachedZoneUuids().iterator().next());
        TimeUnit.SECONDS.sleep(2);
        content = api.search(msg);
        invs = JSONObjectUtil.toCollection(content, ArrayList.class, BackupStorageInventory.class);
        backup2 = invs.get(0);
        Assert.assertEquals(0, backup2.getAttachedZoneUuids().size());

        APIGetBackupStorageMsg gmsg = new APIGetBackupStorageMsg();
        gmsg.setUuid(backup2.getUuid());
        String res = api.getInventory(gmsg);
        BackupStorageInventory binv = JSONObjectUtil.toObject(res, BackupStorageInventory.class);
        Assert.assertEquals(backup2.getName(), binv.getName());
    }

}
