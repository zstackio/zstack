package org.zstack.test.kvm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.securitygroup.SecurityGroupInventory;
import org.zstack.network.securitygroup.VmNicSecurityGroupRefVO;
import org.zstack.network.securitygroup.VmNicSecurityGroupRefVO_;
import org.zstack.network.service.eip.EipInventory;
import org.zstack.network.service.eip.EipVO;
import org.zstack.network.service.portforwarding.PortForwardingRuleInventory;
import org.zstack.network.service.portforwarding.PortForwardingRuleVO;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.virtualrouter.eip.EipTO;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

/**
 * @author frank
 *         <p>
 *         1. create a vm
 *         2. set eip
 *         3. add a port forwarding
 *         4. add a security group
 *         4. detach the eip nic
 *         <p>
 *         confirm eip removed
 *         confirm port forwarding and security group are still avaiable
 */
public class TestDetachNicOnKvm6 {
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
        deployer = new Deployer("deployerXml/kvm/TestDetachNic6.xml", con);
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("VirtualRouterSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("vip.xml");
        deployer.addSpringConfig("eip.xml");
        deployer.addSpringConfig("PortForwarding.xml");
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
        L3NetworkInventory l3 = deployer.l3Networks.get("GuestNetwork");
        L3NetworkInventory l31 = deployer.l3Networks.get("GuestNetwork1");
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VmNicInventory nic = vm.findNic(l3.getUuid());
        VmNicInventory nic1 = vm.findNic(l31.getUuid());
        SecurityGroupInventory sg = deployer.securityGroups.get("sg");
        api.addVmNicToSecurityGroup(sg.getUuid(), nic1.getUuid());

        EipInventory eip = deployer.eips.get("eip");
        VipVO vipvo = dbf.findByUuid(eip.getVipUuid(), VipVO.class);

        api.detachNic(nic.getUuid());

        Assert.assertEquals(1, vconfig.removedEips.size());
        EipTO to = vconfig.removedEips.get(0);
        Assert.assertEquals(vipvo.getIp(), to.getVipIp());
        EipVO eipvo = dbf.findByUuid(eip.getUuid(), EipVO.class);
        Assert.assertNotNull(eipvo);
        Assert.assertNull(eipvo.getVmNicUuid());

        PortForwardingRuleInventory pf = deployer.portForwardingRules.get("pfRule1");
        PortForwardingRuleVO pfvo = dbf.findByUuid(pf.getUuid(), PortForwardingRuleVO.class);
        Assert.assertEquals(nic1.getUuid(), pfvo.getVmNicUuid());

        SimpleQuery<VmNicSecurityGroupRefVO> q = dbf.createQuery(VmNicSecurityGroupRefVO.class);
        q.add(VmNicSecurityGroupRefVO_.vmNicUuid, Op.EQ, nic1.getUuid());
        Assert.assertTrue(q.isExists());
    }
}
