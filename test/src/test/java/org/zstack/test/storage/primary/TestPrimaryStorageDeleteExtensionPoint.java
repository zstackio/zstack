package org.zstack.test.storage.primary;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.simulator.storage.primary.SimulatorPrimaryStorageDetails;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.*;
import org.zstack.utils.data.SizeUnit;

public class TestPrimaryStorageDeleteExtensionPoint {
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;
    PrimaryStorageDeleteExtension ext;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml").addXml("ZoneManager.xml")
                .addXml("Simulator.xml").addXml("PrimaryStorageManager.xml")
                .addXml("PrimaryStorageDeleteExtension.xml").addXml("ConfigurationManager.xml").addXml("AccountManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        ext = loader.getComponent(PrimaryStorageDeleteExtension.class);
        api = new Api();
        api.startServer();
    }

    @After
    public void tearDown() throws Exception {
        api.stopServer();
    }

    @Test
    public void test() throws ApiSenderException {
        SimulatorPrimaryStorageDetails sp = new SimulatorPrimaryStorageDetails();
        sp.setTotalCapacity(SizeUnit.TERABYTE.toByte(10));
        sp.setAvailableCapacity(sp.getTotalCapacity());
        sp.setUrl("nfs://simulator/primary/");
        ZoneInventory zone = api.createZones(1).get(0);
        sp.setZoneUuid(zone.getUuid());
        PrimaryStorageInventory inv = api.createSimulatoPrimaryStorage(1, sp).get(0);
        ext.setPreventDelete(true);
        try {
            api.deletePrimaryStorage(inv.getUuid());
        } catch (ApiSenderException e) {
        }
        PrimaryStorageVO vo = dbf.findByUuid(inv.getUuid(), PrimaryStorageVO.class);
        Assert.assertNotNull(vo);

        ext.setPreventDelete(false);
        ext.setExpectedUuid(inv.getUuid());
        api.deletePrimaryStorage(inv.getUuid());
        vo = dbf.findByUuid(inv.getUuid(), PrimaryStorageVO.class);
        Assert.assertEquals(null, vo);
        Assert.assertTrue(ext.isBeforeCalled());
        Assert.assertTrue(ext.isAfterCalled());
    }
}
