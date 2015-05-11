package org.zstack.test.virtualrouter;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.network.service.virtualrouter.APIQueryVirtualRouterOfferingMsg;
import org.zstack.network.service.virtualrouter.APIQueryVirtualRouterOfferingReply;
import org.zstack.network.service.virtualrouter.VirtualRouterOfferingInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterOfferingVO;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.search.QueryTestValidator;

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

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        //TODO: fix after changing Date to TimeStamp
        InstanceOfferingInventory inv = deployer.instanceOfferings.values().iterator().next();
        VirtualRouterOfferingVO vo = dbf.findByUuid(inv.getUuid(), VirtualRouterOfferingVO.class);
        VirtualRouterOfferingInventory vrinv = VirtualRouterOfferingInventory.valueOf(vo);
        QueryTestValidator.validateEQ(new APIQueryVirtualRouterOfferingMsg(), api, APIQueryVirtualRouterOfferingReply.class, vrinv);
        QueryTestValidator.validateRandomEQConjunction(new APIQueryVirtualRouterOfferingMsg(), api, APIQueryVirtualRouterOfferingReply.class, vrinv, 3);
    }
}
