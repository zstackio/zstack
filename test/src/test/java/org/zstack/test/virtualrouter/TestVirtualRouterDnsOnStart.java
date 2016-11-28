package org.zstack.test.virtualrouter;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.appliancevm.ApplianceVmConstant;
import org.zstack.appliancevm.ApplianceVmVO;
import org.zstack.appliancevm.ApplianceVmVO_;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import java.util.List;

public class TestVirtualRouterDnsOnStart {
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
        deployer = new Deployer("deployerXml/virtualRouter/virtualRouterDns.xml", con);
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("VirtualRouterSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
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
        SimpleQuery<ApplianceVmVO> q = dbf.createQuery(ApplianceVmVO.class);
        q.select(ApplianceVmVO_.uuid);
        q.add(ApplianceVmVO_.type, Op.EQ, ApplianceVmConstant.APPLIANCE_VM_TYPE);
        q.add(ApplianceVmVO_.applianceVmType, Op.EQ, VirtualRouterConstant.VIRTUAL_ROUTER_VM_TYPE);
        String vruuid = q.findValue();
        api.stopVmInstance(vruuid);
        api.startVmInstance(vruuid);
        List<String> retDns = CollectionUtils.transformToList(vconfig.dnsInfo, new Function<String, VirtualRouterCommands.DnsInfo>() {
            @Override
            public String call(VirtualRouterCommands.DnsInfo arg) {
                return arg.getDnsAddress();
            }
        });
        L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network2");
        // reload l3network making sure all context are in it
        L3NetworkVO l3vo = dbf.findByUuid(l3.getUuid(), L3NetworkVO.class);
        l3 = L3NetworkInventory.valueOf(l3vo);
        for (String dns : l3.getDns()) {
            if (!retDns.contains(dns)) {
                Assert.fail(dns + " is not found");
            }
        }
    }
}
