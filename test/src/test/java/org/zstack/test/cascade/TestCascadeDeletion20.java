package org.zstack.test.cascade;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.appliancevm.ApplianceVmVO;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

/**
 * 1. create a vm with virtual router
 * 2. delete host
 * <p>
 * confirm vr is migrated
 */
public class TestCascadeDeletion20 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/virtualRouter/virtualRouterSNAT2.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("VirtualRouterSimulator.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        api.setTimeout(100000);
        ApplianceVmVO vr = dbf.listAll(ApplianceVmVO.class).get(0);
        String lastHostUuid = vr.getHostUuid();
        api.deleteHost(lastHostUuid);
        vr = dbf.listAll(ApplianceVmVO.class).get(0);
        Assert.assertEquals(VmInstanceState.Running, vr.getState());
        Assert.assertFalse(lastHostUuid.equals(vr.getHostUuid()));

        long count = dbf.count(VmInstanceVO.class);
        Assert.assertEquals(2, count);
    }
}
