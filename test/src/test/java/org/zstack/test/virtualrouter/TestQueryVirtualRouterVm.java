package org.zstack.test.virtualrouter;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.appliancevm.APIQueryApplianceVmMsg;
import org.zstack.appliancevm.APIQueryApplianceVmReply;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.query.QueryOp;
import org.zstack.header.vm.APIQueryVmInstanceMsg;
import org.zstack.header.vm.APIQueryVmInstanceReply;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicVO;
import org.zstack.network.service.virtualrouter.APIQueryVirtualRouterVmMsg;
import org.zstack.network.service.virtualrouter.APIQueryVirtualRouterVmReply;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;
import org.zstack.simulator.appliancevm.ApplianceVmSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

import java.util.ArrayList;

public class TestQueryVirtualRouterVm {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    VirtualRouterSimulatorConfig vconfig;
    ApplianceVmSimulatorConfig aconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/virtualRouter/TestVirtualRouterPortForwarding2.xml", con);
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("VirtualRouterSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("PortForwarding.xml");
        deployer.addSpringConfig("vip.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        vconfig = loader.getComponent(VirtualRouterSimulatorConfig.class);
        aconfig = loader.getComponent(ApplianceVmSimulatorConfig.class);
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        VirtualRouterVmVO vr = dbf.listAll(VirtualRouterVmVO.class).get(0);
        VmNicVO nic = vr.getVmNics().iterator().next();

        APIQueryVirtualRouterVmMsg msg = new APIQueryVirtualRouterVmMsg();
        msg.addQueryCondition("vmNics.ip", QueryOp.EQ, nic.getIp());
        APIQueryVirtualRouterVmReply reply = api.query(msg, APIQueryVirtualRouterVmReply.class);
        Assert.assertEquals(1, reply.getInventories().size());
        VirtualRouterVmInventory vrinv = reply.getInventories().get(0);
        Assert.assertEquals(vr.getUuid(), vrinv.getUuid());

        APIQueryApplianceVmMsg qmsg = new APIQueryApplianceVmMsg();
        qmsg.addQueryCondition("vmNics.ip", QueryOp.EQ, nic.getIp());
        APIQueryApplianceVmReply qr = api.query(qmsg, APIQueryApplianceVmReply.class);
        Assert.assertEquals(1, qr.getInventories().size());
        vrinv = reply.getInventories().get(0);
        Assert.assertEquals(vr.getUuid(), vrinv.getUuid());

        APIQueryVmInstanceMsg amsg = new APIQueryVmInstanceMsg();
        amsg.setConditions(new ArrayList<QueryCondition>());
        APIQueryVmInstanceReply areply = api.query(amsg, APIQueryVmInstanceReply.class);

        Assert.assertEquals(2, areply.getInventories().size());

        for (VmInstanceInventory inv : areply.getInventories()) {
            if (inv.getUuid().equals(vr.getUuid())) {
                Assert.assertTrue(inv instanceof VirtualRouterVmInventory);
                return;
            }
        }

        Assert.fail("APIQueryVmInstanceMsg returns no virtual router vm");
    }
}
