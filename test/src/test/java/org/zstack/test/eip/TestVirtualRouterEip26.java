package org.zstack.test.eip;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.appliancevm.ApplianceVmVO;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.network.service.eip.EipInventory;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.VipTO;
import org.zstack.network.service.virtualrouter.eip.EipTO;
import org.zstack.network.service.virtualrouter.eip.VirtualRouterEipRefVO;
import org.zstack.network.service.virtualrouter.eip.VirtualRouterEipRefVO_;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

import java.util.List;

/**
 * @author frank
 * @condition 1. create a vm
 * 2. destroy vr
 * 3. create new vr
 * @test confirm eip is on new vr
 */
public class TestVirtualRouterEip26 {
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
        ApplianceVmVO vr = dbf.listAll(ApplianceVmVO.class).get(0);
        api.destroyVmInstance(vr.getUuid());

        vconfig.eips.clear();
        vconfig.vips.clear();
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        api.createVmFromClone(vm);

        vr = dbf.listAll(ApplianceVmVO.class).get(0);
        SimpleQuery<VirtualRouterEipRefVO> q = dbf.createQuery(VirtualRouterEipRefVO.class);
        q.add(VirtualRouterEipRefVO_.virtualRouterVmUuid, Op.EQ, vr.getUuid());
        List<VirtualRouterEipRefVO> refs = q.list();
        Assert.assertEquals(1, refs.size());

        Assert.assertEquals(1, vconfig.eips.size());
        EipTO to = vconfig.eips.get(0);
        EipInventory eip = deployer.eips.get("eip");
        VipVO vipvo = dbf.findByUuid(eip.getVipUuid(), VipVO.class);
        Assert.assertEquals(vipvo.getIp(), to.getVipIp());

        Assert.assertEquals(1, vconfig.vips.size());
        VipTO vipto = vconfig.vips.get(0);
        Assert.assertEquals(vipvo.getIp(), vipto.getIp());
    }
}
