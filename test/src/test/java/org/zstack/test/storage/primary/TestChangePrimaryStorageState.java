package org.zstack.test.storage.primary;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.simulator.storage.primary.SimulatorPrimaryStorageDetails;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageState;
import org.zstack.header.storage.primary.PrimaryStorageStateEvent;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.BeanConstructor;
import org.zstack.test.DBUtil;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.BrokenBarrierException;
public class TestChangePrimaryStorageState {
    CLogger logger = Utils.getLogger(TestChangePrimaryStorageState.class);
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new BeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml").addXml("ZoneManager.xml")
                .addXml("Simulator.xml").addXml("PrimaryStorageManager.xml").addXml("ConfigurationManager.xml").addXml("AccountManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        api = new Api();
        api.startServer();
    }


    @Test
    public void test() throws ApiSenderException, InterruptedException, BrokenBarrierException {
        SimulatorPrimaryStorageDetails sp = new SimulatorPrimaryStorageDetails();
        sp.setTotalCapacity(SizeUnit.TERABYTE.toByte(10));
        sp.setAvailableCapacity(sp.getTotalCapacity());
        sp.setUrl("nfs://simulator/primary/");
    	ZoneInventory zone = api.createZones(1).get(0);
        sp.setZoneUuid(zone.getUuid());
        PrimaryStorageInventory inv = api.createSimulatoPrimaryStorage(1, sp).get(0);
        inv = api.changePrimaryStorageState(inv.getUuid(), PrimaryStorageStateEvent.disable);
        Assert.assertEquals(PrimaryStorageState.Disabled.toString(), inv.getState());
        inv = api.changePrimaryStorageState(inv.getUuid(), PrimaryStorageStateEvent.enable);
        Assert.assertEquals(PrimaryStorageState.Enabled.toString(), inv.getState());
        inv = api.changePrimaryStorageState(inv.getUuid(), PrimaryStorageStateEvent.maintain);
        Assert.assertEquals(PrimaryStorageState.Maintenance.toString(), inv.getState());
    }
}
