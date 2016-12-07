package org.zstack.test.configuration;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.DiskOfferingState;
import org.zstack.header.configuration.DiskOfferingStateEvent;
import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.test.*;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 3:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class TestChangeDiskOfferingState {
    CLogger logger = Utils.getLogger(TestChangeDiskOfferingState.class);
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
        DiskOfferingVO vo = dbf.findByUuid(inv.getUuid(), DiskOfferingVO.class);
        Assert.assertEquals(SizeUnit.GIGABYTE.toByte(10), vo.getDiskSize());
        Assert.assertEquals(DiskOfferingState.Enabled.toString(), inv.getState());
        inv = api.changeDiskOfferingState(inv.getUuid(), DiskOfferingStateEvent.disable);
        Assert.assertEquals(DiskOfferingState.Disabled.toString(), inv.getState());
        inv = api.changeDiskOfferingState(inv.getUuid(), DiskOfferingStateEvent.enable);
        Assert.assertEquals(DiskOfferingState.Enabled.toString(), inv.getState());
    }
}
