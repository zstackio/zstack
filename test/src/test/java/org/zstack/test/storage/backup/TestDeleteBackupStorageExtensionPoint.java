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
import org.zstack.test.*;
import org.zstack.utils.data.SizeUnit;

public class TestDeleteBackupStorageExtensionPoint {
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;
    BackupStorageDeleteExtension ext;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml")
                .addXml("Simulator.xml").addXml("BackupStorageManager.xml").addXml("BackupStorageDeleteExtension.xml").addXml("AccountManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        ext = loader.getComponent(BackupStorageDeleteExtension.class);
        api = new Api();
        api.startServer();
    }

    @After
    public void tearDown() throws Exception {
        api.stopServer();
    }

    @Test
    public void test() throws ApiSenderException {
        SimulatorBackupStorageDetails ss = new SimulatorBackupStorageDetails();
        ss.setTotalCapacity(SizeUnit.GIGABYTE.toByte(100));
        ss.setUsedCapacity(0);
        ss.setUrl("nfs://simulator/backupstorage/");
        BackupStorageInventory inv = api.createSimulatorBackupStorage(1, ss).get(0);
        ext.setPreventDelete(true);
        try {
            api.deleteBackupStorage(inv.getUuid());
        } catch (ApiSenderException e) {
        }
        BackupStorageVO vo = dbf.findByUuid(inv.getUuid(), BackupStorageVO.class);
        Assert.assertNotNull(vo);

        ext.setExpectedBackupStorageUuid(inv.getUuid());
        ext.setPreventDelete(false);
        api.deleteBackupStorage(inv.getUuid());
        Assert.assertTrue(ext.isBeforeCalled());
        Assert.assertTrue(ext.isAfterCalled());
        vo = dbf.findByUuid(inv.getUuid(), BackupStorageVO.class);
        Assert.assertEquals(null, vo);
    }
}
