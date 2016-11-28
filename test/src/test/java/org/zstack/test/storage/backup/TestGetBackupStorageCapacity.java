package org.zstack.test.storage.backup;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.storage.backup.APIGetBackupStorageCapacityReply;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestGetBackupStorageCapacity {
    CLogger logger = Utils.getLogger(TestGetBackupStorageCapacity.class);
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
        ZoneInventory zone1 = deployer.zones.get("Zone1");
        BackupStorageInventory bs = deployer.backupStorages.get("backup1");

        List<BackupStorageVO> bsvos = dbf.listAll(BackupStorageVO.class);
        long total = 0;
        long avail = 0;
        List<String> bsUuids = new ArrayList<String>();
        for (BackupStorageVO bsvo : bsvos) {
            total += bsvo.getTotalCapacity();
            avail += bsvo.getAvailableCapacity();
            bsUuids.add(bsvo.getUuid());
        }

        APIGetBackupStorageCapacityReply reply = api.getBackupStorageCapacity(null, bsUuids);
        Assert.assertEquals(total, reply.getTotalCapacity());
        Assert.assertEquals(avail, reply.getAvailableCapacity());

        reply = api.getBackupStorageCapacityByAll();
        Assert.assertEquals(total, reply.getTotalCapacity());
        Assert.assertEquals(avail, reply.getAvailableCapacity());

        BackupStorageVO bsvo = dbf.findByUuid(bs.getUuid(), BackupStorageVO.class);
        reply = api.getBackupStorageCapacity(null, Arrays.asList(bsvo.getUuid()));
        Assert.assertEquals(bsvo.getTotalCapacity(), reply.getTotalCapacity());
        Assert.assertEquals(bsvo.getAvailableCapacity(), reply.getAvailableCapacity());

        reply = api.getBackupStorageCapacity(Arrays.asList(zone1.getUuid()), null);
        Assert.assertEquals(bsvo.getTotalCapacity(), reply.getTotalCapacity());
        Assert.assertEquals(bsvo.getAvailableCapacity(), reply.getAvailableCapacity());
    }
}
