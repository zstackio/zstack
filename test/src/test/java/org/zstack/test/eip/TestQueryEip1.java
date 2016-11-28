package org.zstack.test.eip;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.query.QueryOp;
import org.zstack.header.vm.VmNicVO;
import org.zstack.network.service.eip.APIQueryEipMsg;
import org.zstack.network.service.eip.APIQueryEipReply;
import org.zstack.network.service.eip.EipInventory;
import org.zstack.network.service.vip.VipVO;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

/**
 * test expanded query
 */
public class TestQueryEip1 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    VirtualRouterSimulatorConfig vconfig;
    KVMSimulatorConfig kconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/eip/TestVirtualRouterEip.xml", con);
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("VirtualRouterSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("vip.xml");
        deployer.addSpringConfig("eip.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        vconfig = loader.getComponent(VirtualRouterSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        EipInventory eip = deployer.eips.get("eip");

        VipVO vip = dbf.findByUuid(eip.getVipUuid(), VipVO.class);
        APIQueryEipMsg msg = new APIQueryEipMsg();
        msg.setReplyWithCount(true);
        msg.addQueryCondition("vip.ip", QueryOp.EQ, vip.getIp());
        APIQueryEipReply reply = api.query(msg, APIQueryEipReply.class);
        Assert.assertEquals(1, reply.getInventories().size());
        EipInventory qeip = reply.getInventories().get(0);
        Assert.assertEquals(eip.getUuid(), qeip.getUuid());
        Assert.assertEquals(1L, (long) reply.getTotal());

        VmNicVO nic = dbf.findByUuid(eip.getVmNicUuid(), VmNicVO.class);
        msg = new APIQueryEipMsg();
        msg.setReplyWithCount(true);
        msg.addQueryCondition("vmNic.uuid", QueryOp.EQ, nic.getUuid());
        reply = api.query(msg, APIQueryEipReply.class);
        Assert.assertEquals(1, reply.getInventories().size());
        qeip = reply.getInventories().get(0);
        Assert.assertEquals(eip.getUuid(), qeip.getUuid());
        Assert.assertEquals(1L, (long) reply.getTotal());

        msg = new APIQueryEipMsg();
        msg.setReplyWithCount(true);
        msg.addQueryCondition("vmNic.uuid", QueryOp.EQ, nic.getUuid());
        msg.addQueryCondition("uuid", QueryOp.EQ, eip.getUuid());
        reply = api.query(msg, APIQueryEipReply.class);
        Assert.assertEquals(1, reply.getInventories().size());
        qeip = reply.getInventories().get(0);
        Assert.assertEquals(eip.getUuid(), qeip.getUuid());
        Assert.assertEquals(1L, (long) reply.getTotal());

        msg = new APIQueryEipMsg();
        msg.setReplyWithCount(true);
        msg.addQueryCondition("vmNic.ip", QueryOp.NOT_EQ, nic.getIp());
        reply = api.query(msg, APIQueryEipReply.class);
        Assert.assertEquals(0, reply.getInventories().size());
        Assert.assertEquals(0L, (long) reply.getTotal());
    }
}
