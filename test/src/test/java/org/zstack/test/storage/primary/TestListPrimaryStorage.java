package org.zstack.test.storage.primary;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.simulator.storage.primary.SimulatorPrimaryStorageDetails;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import java.util.List;

public class TestListPrimaryStorage {
    CLogger logger = Utils.getLogger(TestListPrimaryStorage.class);
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml").addXml("ZoneManager.xml")
                .addXml("Simulator.xml").addXml("PrimaryStorageManager.xml").addXml("ConfigurationManager.xml").addXml("AccountManager.xml").build();
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
        SimulatorPrimaryStorageDetails sp = new SimulatorPrimaryStorageDetails();
        sp.setTotalCapacity(SizeUnit.TERABYTE.toByte(10));
        sp.setAvailableCapacity(sp.getTotalCapacity());
        sp.setUrl("nfs://simulator/primary/");
        ZoneInventory zone = api.createZones(1).get(0);
        sp.setZoneUuid(zone.getUuid());
        api.createSimulatoPrimaryStorage(10, sp);
        List<PrimaryStorageInventory> invs = api.listPrimaryStorage(null);
        Assert.assertEquals(10, invs.size());
        List<String> uuids = CollectionUtils.transformToList(invs, new Function<String, PrimaryStorageInventory>() {
            @Override
            public String call(PrimaryStorageInventory arg) {
                return arg.getUuid();
            }
        });
        invs = api.listPrimaryStorage(uuids);
        for (int i = 0; i < uuids.size(); i++) {
            Assert.assertEquals(uuids.get(i), invs.get(i).getUuid());
        }
    }
}
