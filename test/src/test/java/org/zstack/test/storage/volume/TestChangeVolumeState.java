package org.zstack.test.storage.volume;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.volume.*;
import org.zstack.test.*;
import org.zstack.utils.data.SizeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 9:54 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestChangeVolumeState {
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;
    CloudBus bus;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml").addXml("Simulator.xml")
                .addXml("PrimaryStorageManager.xml").addXml("ConfigurationManager.xml")
                .addXml("VolumeManager.xml").addXml("ZoneManager.xml").addXml("AccountManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        bus = loader.getComponent(CloudBus.class);
        api = new Api();
        api.startServer();
    }

    @After
    public void tearDown() throws Exception {
        api.stopServer();
    }

    @Test
    public void test() throws ApiSenderException {
        DiskOfferingInventory dinv = new DiskOfferingInventory();
        dinv.setDiskSize(SizeUnit.GIGABYTE.toByte(10));
        dinv.setName("Test");
        dinv.setDescription("Test");
        dinv = api.addDiskOffering(dinv);

        VolumeInventory vinv = api.createDataVolume("TestData", dinv.getUuid());
        Assert.assertEquals(VolumeStatus.NotInstantiated.toString(), vinv.getStatus());
        Assert.assertEquals(VolumeType.Data.toString(), vinv.getType());
        Assert.assertFalse(vinv.isAttached());
        Assert.assertEquals(VolumeState.Enabled.toString(), vinv.getState());

        vinv = api.changeVolumeState(vinv.getUuid(), VolumeStateEvent.disable);
        Assert.assertEquals(VolumeState.Disabled.toString(), vinv.getState());
        vinv = api.changeVolumeState(vinv.getUuid(), VolumeStateEvent.enable);
        Assert.assertEquals(VolumeState.Enabled.toString(), vinv.getState());
    }
}
