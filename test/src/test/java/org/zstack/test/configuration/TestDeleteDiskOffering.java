package org.zstack.test.configuration;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.test.*;
import org.zstack.test.image.TestAddImage;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

public class TestDeleteDiskOffering {
    CLogger logger = Utils.getLogger(TestAddImage.class);
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml").addXml("ConfigurationManager.xml").addXml("ZoneManager.xml")
                .addXml("Simulator.xml").addXml("PrimaryStorageManager.xml").addXml("AccountManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        api = new Api();
        api.startServer();
    }

    @After
    public void tearDown() throws Exception {
        api.stopServer();
    }

    @Test
    public void test() throws InterruptedException, ApiSenderException {
        DiskOfferingInventory inv = new DiskOfferingInventory();
        inv.setDiskSize(SizeUnit.GIGABYTE.toByte(10));
        inv.setName("Test");
        inv.setDescription("Test");
        inv = api.addDiskOffering(inv);
        api.deleteDiskOffering(inv.getUuid());
        DiskOfferingVO vo = dbf.findByUuid(inv.getUuid(), DiskOfferingVO.class);
        Assert.assertEquals(null, vo);
    }
}
