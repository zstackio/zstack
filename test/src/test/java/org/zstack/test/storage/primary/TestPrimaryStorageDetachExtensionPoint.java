package org.zstack.test.storage.primary;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.simulator.storage.primary.SimulatorPrimaryStorageDetails;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.*;
import org.zstack.utils.data.SizeUnit;

public class TestPrimaryStorageDetachExtensionPoint {
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;
    PrimaryStorageDetachExtension ext;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml").addXml("Simulator.xml").addXml("PrimaryStorageManager.xml")
                .addXml("ZoneManager.xml").addXml("ClusterManager.xml")
                .addXml("PrimaryStorageDetachExtension.xml").addXml("ConfigurationManager.xml").addXml("AccountManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        ext = loader.getComponent(PrimaryStorageDetachExtension.class);
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
        ClusterInventory cluster = api.createClusters(1, zone.getUuid()).get(0);
        SimulatorPrimaryStorageDetails sp = new SimulatorPrimaryStorageDetails();
        sp.setTotalCapacity(SizeUnit.TERABYTE.toByte(10));
        sp.setAvailableCapacity(sp.getTotalCapacity());
        sp.setUrl("nfs://simulator/primary/");
        sp.setZoneUuid(zone.getUuid());
        PrimaryStorageInventory inv = api.createSimulatoPrimaryStorage(1, sp).get(0);
        api.attachPrimaryStorage(cluster.getUuid(), inv.getUuid());

        ext.setPreventDetach(true);
        try {
            api.detachPrimaryStorage(inv.getUuid(), cluster.getUuid());
        } catch (ApiSenderException e) {
        }

        ext.setPreventDetach(false);
        ext.setExpectedClusterUuid(cluster.getUuid());
        ext.setExpectedPrimaryStorageUuid(inv.getUuid());
        api.detachPrimaryStorage(inv.getUuid(), cluster.getUuid());
        Assert.assertTrue(ext.isBeforeCalled());
        Assert.assertTrue(ext.isAfterCalled());
    }
}
