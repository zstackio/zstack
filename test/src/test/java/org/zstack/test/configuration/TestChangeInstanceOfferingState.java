package org.zstack.test.configuration;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.*;
import org.zstack.test.*;
import org.zstack.test.image.TestAddImage;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

public class TestChangeInstanceOfferingState {
    CLogger logger = Utils.getLogger(TestAddImage.class);
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml")
                .addXml("ConfigurationManager.xml").addXml("HostAllocatorManager.xml").addXml("AccountManager.xml").build();
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
        InstanceOfferingInventory inv = new InstanceOfferingInventory();
        inv.setName("TestInstanceOffering");
        inv.setCpuNum(2);
        inv.setCpuSpeed(1000);
        inv.setMemorySize(SizeUnit.GIGABYTE.toByte(1));
        inv.setDescription("TestInstanceOffering");
        inv = api.addInstanceOffering(inv);

        InstanceOfferingVO vo = dbf.findByUuid(inv.getUuid(), InstanceOfferingVO.class);
        Assert.assertNotNull(vo);
        Assert.assertEquals(InstanceOfferingDuration.Permanent, vo.getDuration());
        Assert.assertEquals(InstanceOfferingState.Enabled, vo.getState());

        inv = api.changeInstanceOfferingState(inv.getUuid(), InstanceOfferingStateEvent.disable);
        Assert.assertEquals(InstanceOfferingState.Disabled.toString(), inv.getState());
        inv = api.changeInstanceOfferingState(inv.getUuid(), InstanceOfferingStateEvent.enable);
        Assert.assertEquals(InstanceOfferingState.Enabled.toString(), inv.getState());
    }

}
