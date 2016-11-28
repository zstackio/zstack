package org.zstack.test.storage.backup;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.*;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

/*
 * 1. add backup storage with 1G available capacity
 * 2. detach from zone
 * 3. allocate 500M with attached zone uuid
 *
 * confirm:
 *  fail
 */
public class TestAllocateBackupStorage6 {
    CLogger logger = Utils.getLogger(TestAllocateBackupStorage6.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/backupStorage/TestAllocateBackupStorage.xml", con);
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        BackupStorageInventory bsinv = deployer.backupStorages.get("backup1");
        ZoneInventory zone = deployer.zones.get("Zone1");

        api.detachBackupStorage(bsinv.getUuid(), zone.getUuid());
        long size = SizeUnit.MEGABYTE.toByte(500);
        AllocateBackupStorageMsg msg = new AllocateBackupStorageMsg();
        msg.setSize(size);
        msg.setRequiredZoneUuid(zone.getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, BackupStorageConstant.SERVICE_ID, bsinv.getUuid());
        msg.setTimeout(TimeUnit.SECONDS.toMillis(15));
        MessageReply reply = bus.call(msg);
        Assert.assertFalse(reply.isSuccess());
        Assert.assertEquals(BackupStorageErrors.ALLOCATE_ERROR.toString(), reply.getError().getCode());
        BackupStorageVO vo = dbf.findByUuid(bsinv.getUuid(), BackupStorageVO.class);
        Assert.assertEquals(bsinv.getAvailableCapacity(), vo.getAvailableCapacity());
    }
}
