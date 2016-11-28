package org.zstack.test.virtualrouter;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.APICreateInstanceOfferingEvent;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.network.service.virtualrouter.*;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.search.QueryTestValidator;
import org.zstack.utils.data.SizeUnit;

public class TestQueryVirtualRouterOffering {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/virtualRouter/TestQueryVirtualRouterOffering.xml", con);
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    InstanceOfferingInventory createOffering() throws ApiSenderException {
        ZoneInventory zone = deployer.zones.get("Zone1");
        L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network1");
        ImageInventory img = deployer.images.get("TestImage");

        APICreateVirtualRouterOfferingMsg msg = new APICreateVirtualRouterOfferingMsg();
        msg.setName("vr");
        msg.setImageUuid(img.getUuid());
        msg.setManagementNetworkUuid(l3.getUuid());
        msg.setPublicNetworkUuid(l3.getUuid());
        msg.setZoneUuid(zone.getUuid());
        msg.setCpuNum(1);
        msg.setCpuSpeed(1);
        msg.setMemorySize(SizeUnit.GIGABYTE.toByte(1));
        msg.setSession(api.getAdminSession());
        ApiSender sender = api.getApiSender();
        APICreateInstanceOfferingEvent evt = sender.send(msg, APICreateInstanceOfferingEvent.class);
        return evt.getInventory();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        InstanceOfferingInventory inv = createOffering();
        VirtualRouterOfferingVO vo = dbf.findByUuid(inv.getUuid(), VirtualRouterOfferingVO.class);
        VirtualRouterOfferingInventory vrinv = VirtualRouterOfferingInventory.valueOf(vo);
        QueryTestValidator.validateEQ(new APIQueryVirtualRouterOfferingMsg(), api, APIQueryVirtualRouterOfferingReply.class, vrinv);
        QueryTestValidator.validateRandomEQConjunction(new APIQueryVirtualRouterOfferingMsg(), api, APIQueryVirtualRouterOfferingReply.class, vrinv, 3);
    }
}
