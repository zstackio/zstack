package org.zstack.test.configuration;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.test.*;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

public class TestUpdateDiskOffering {
    CLogger logger = Utils.getLogger(TestUpdateDiskOffering.class);
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("ZoneManager.xml").addXml("PortalForUnitTest.xml").addXml("ConfigurationManager.xml").addXml("Simulator.xml").addXml("PrimaryStorageManager.xml").addXml("AccountManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        api = new Api();
        api.startServer();
    }

    @Test
    public void test() throws InterruptedException, ApiSenderException {
        DiskOfferingInventory inv = new DiskOfferingInventory();
        inv.setDiskSize(SizeUnit.GIGABYTE.toByte(10));
        inv.setName("Test");
        inv.setDescription("Test");
        inv = api.addDiskOffering(inv);
        inv.setName("1");
        inv.setDescription("xxx");
        inv = api.updateDiskOffering(inv);
        Assert.assertEquals("1", inv.getName());
        Assert.assertEquals("xxx", inv.getDescription());
    }
}
