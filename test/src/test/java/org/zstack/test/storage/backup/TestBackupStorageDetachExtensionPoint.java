package org.zstack.test.storage.backup;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.simulator.storage.backup.SimulatorBackupStorageDetails;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.*;
import org.zstack.utils.data.SizeUnit;

public class TestBackupStorageDetachExtensionPoint {
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;
    BackupStorageDetachExtension ext;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml").addXml("Simulator.xml")
                .addXml("BackupStorageManager.xml").addXml("ZoneManager.xml").addXml("BackupStorageDetachExtension.xml").addXml("AccountManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        ext = loader.getComponent(BackupStorageDetachExtension.class);
        api = new Api();
        api.startServer();
    }

    @After
    public void tearDown() throws Exception {
        api.stopServer();
    }

    @Test
    public void test() throws ApiSenderException {
        ZoneInventory zone = api.createZones(1).get(0);
        SimulatorBackupStorageDetails ss = new SimulatorBackupStorageDetails();
        ss.setTotalCapacity(SizeUnit.GIGABYTE.toByte(100));
        ss.setUsedCapacity(0);
        ss.setUrl("nfs://simulator/backupstorage/");
        BackupStorageInventory inv = api.createSimulatorBackupStorage(1, ss).get(0);
        inv = api.attachBackupStorage(zone.getUuid(), inv.getUuid());
        BackupStorageVO vo = dbf.findByUuid(inv.getUuid(), BackupStorageVO.class);
        Assert.assertEquals(zone.getUuid(), vo.getAttachedZoneRefs().iterator().next().getZoneUuid());
        Assert.assertEquals(zone.getUuid(), inv.getAttachedZoneUuids().iterator().next());

        ext.setPreventChange(true);
        try {
            api.detachBackupStorage(inv.getUuid(), zone.getUuid());
        } catch (ApiSenderException e) {
        }
        vo = dbf.findByUuid(inv.getUuid(), BackupStorageVO.class);
        Assert.assertEquals(1, vo.getAttachedZoneRefs().size());

        ext.setPreventChange(false);
        ext.setExpectedBackStorageUuid(inv.getUuid());
        ext.setExpectedZoneUuid(zone.getUuid());
        inv = api.detachBackupStorage(inv.getUuid(), zone.getUuid());
        vo = dbf.findByUuid(inv.getUuid(), BackupStorageVO.class);
        Assert.assertEquals(0, vo.getAttachedZoneRefs().size());
        Assert.assertEquals(0, inv.getAttachedZoneUuids().size());
        Assert.assertTrue(ext.isBeforeCalled());
        Assert.assertTrue(ext.isAfterCalled());
    }


}
