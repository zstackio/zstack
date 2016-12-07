package org.zstack.test.compute.host;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStateEvent;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.*;

public class TestChangeHostStateExtensionPoint {
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;
    ChangeHostStateExtension ext;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml")
                .addXml("ClusterManager.xml")
                .addXml("ZoneManager.xml")
                .addXml("HostManager.xml")
                .addXml("Simulator.xml")
                .addXml("ChangeHostStateExtension.xml")
                .addXml("ChangeHostStateExtension.xml")
                .addXml("HostAllocatorManager.xml")
                .addXml("AccountManager.xml")
                .build();
        dbf = loader.getComponent(DatabaseFacade.class);
        ext = loader.getComponent(ChangeHostStateExtension.class);
        api = new Api();
        api.startServer();
    }

    @Test
    public void test() throws ApiSenderException {
        try {
            ZoneInventory zone = api.createZones(1).get(0);
            ClusterInventory cluster = api.createClusters(1, zone.getUuid()).get(0);
            HostInventory host = api.createHost(1, cluster.getUuid()).get(0);
            ext.setPreventChange(true);
            try {
                api.changeHostState(host.getUuid(), HostStateEvent.disable);
            } catch (ApiSenderException e) {
            }

            ext.setExpectedCurrent(HostState.Enabled);
            ext.setExpectedNext(HostState.Disabled);
            ext.setExpectedStateEvent(HostStateEvent.disable);
            ext.setPreventChange(false);
            api.changeHostState(host.getUuid(), HostStateEvent.disable);
            host = api.listHosts(null).get(0);
            Assert.assertEquals(HostState.Disabled.toString(), host.getState());
            Assert.assertTrue(ext.isBeforeCalled());
            Assert.assertTrue(ext.isAfterCalled());
        } finally {
            api.stopServer();
        }
    }

}
