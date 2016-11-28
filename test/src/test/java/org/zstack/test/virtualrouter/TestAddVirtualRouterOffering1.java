package org.zstack.test.virtualrouter;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterOfferingVO;
import org.zstack.test.Api;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

public class TestAddVirtualRouterOffering1 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/virtualRouter/virtualRouterOffering1.xml", con);
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() {
        InstanceOfferingInventory iinv = deployer.instanceOfferings.get("virtualRouterOffering");
        VirtualRouterOfferingVO vo = dbf.findByUuid(iinv.getUuid(), VirtualRouterOfferingVO.class);
        Assert.assertNotNull(vo);
        Assert.assertTrue(vo.isDefault());

        iinv = deployer.instanceOfferings.get("virtualRouterOffering2");
        vo = dbf.findByUuid(iinv.getUuid(), VirtualRouterOfferingVO.class);
        Assert.assertNotNull(vo);
        Assert.assertTrue(vo.isDefault());
    }
}
