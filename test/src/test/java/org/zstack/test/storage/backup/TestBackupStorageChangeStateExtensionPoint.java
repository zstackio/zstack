package org.zstack.test.storage.backup;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.simulator.storage.backup.SimulatorBackupStorageDetails;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStorageState;
import org.zstack.header.storage.backup.BackupStorageStateEvent;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.test.*;
import org.zstack.utils.data.SizeUnit;

public class TestBackupStorageChangeStateExtensionPoint {
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;
    BackupStorageChangeStateExtension ext;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml")
                .addXml("Simulator.xml").addXml("BackupStorageManager.xml").addXml("BackupStorageChangeStateExtension.xml").addXml("AccountManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        ext = loader.getComponent(BackupStorageChangeStateExtension.class);
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

        ext.setPreventChange(true);
        try {
            api.changeBackupStorageState(inv.getUuid(), BackupStorageStateEvent.disable);
        } catch (ApiSenderException e) {
        }
        BackupStorageVO vo = dbf.findByUuid(inv.getUuid(), BackupStorageVO.class);
        Assert.assertEquals(BackupStorageState.Enabled, vo.getState());

        ext.setPreventChange(false);
        ext.setExpectedBackupStorageUuid(inv.getUuid());
        ext.setExpectedCurrent(BackupStorageState.Enabled);
        ext.setExpectedStateEvent(BackupStorageStateEvent.disable);
        ext.setExpectedNext(BackupStorageState.Disabled);
        api.changeBackupStorageState(inv.getUuid(), BackupStorageStateEvent.disable);
        vo = dbf.findByUuid(inv.getUuid(), BackupStorageVO.class);
        Assert.assertEquals(BackupStorageState.Disabled, vo.getState());
        Assert.assertTrue(ext.isBeforeCalled());
        Assert.assertTrue(ext.isAfterCalled());
    }
}
