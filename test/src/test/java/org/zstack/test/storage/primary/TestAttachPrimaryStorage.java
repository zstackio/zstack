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
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

public class TestAttachPrimaryStorage {
    CLogger logger = Utils.getLogger(TestCreatePrimaryStorage.class);
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml")
                .addXml("Simulator.xml").addXml("PrimaryStorageManager.xml")
                .addXml("ZoneManager.xml").addXml("ClusterManager.xml").addXml("ConfigurationManager.xml").addXml("AccountManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
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
        List<ClusterInventory> clusters = api.createClusters(5, zone.getUuid());
        SimulatorPrimaryStorageDetails sp = new SimulatorPrimaryStorageDetails();
        sp.setTotalCapacity(SizeUnit.TERABYTE.toByte(10));
        sp.setAvailableCapacity(sp.getTotalCapacity());
        sp.setUrl("nfs://simulator/primary/");
        sp.setZoneUuid(zone.getUuid());
        PrimaryStorageInventory inv = api.createSimulatoPrimaryStorage(1, sp).get(0);
        for (ClusterInventory c : clusters) {
            api.attachPrimaryStorage(c.getUuid(), inv.getUuid());
        }
        List<String> uuids = new ArrayList<String>(1);
        uuids.add(inv.getUuid());
        inv = api.listPrimaryStorage(uuids).get(0);
        Assert.assertEquals(clusters.size(), inv.getAttachedClusterUuids().size());
    }
}
