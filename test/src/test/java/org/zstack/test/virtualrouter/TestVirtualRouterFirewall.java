package org.zstack.test.virtualrouter;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.appliancevm.ApplianceVmFirewallProtocol;
import org.zstack.appliancevm.ApplianceVmFirewallRuleTO;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.apimediator.ApiMediatorConstant;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.APICreateVmInstanceEvent;
import org.zstack.header.vm.APICreateVmInstanceMsg;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterVmInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;
import org.zstack.simulator.appliancevm.ApplianceVmSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.RangeSet.Range;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

public class TestVirtualRouterFirewall {
    CLogger logger = Utils.getLogger(TestVirtualRouterFirewall.class);
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
        deployer = new Deployer("deployerXml/virtualRouter/startVirtualRouter.xml", con);
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("VirtualRouterSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        vconfig = loader.getComponent(VirtualRouterSimulatorConfig.class);
        aconfig = loader.getComponent(ApplianceVmSimulatorConfig.class);
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        session = api.loginAsAdmin();
    }

    private void hasRule(List<ApplianceVmFirewallRuleTO> tos, int startPort, ApplianceVmFirewallProtocol protocol) {
        for (ApplianceVmFirewallRuleTO r : tos) {
            if (protocol.toString().equals(r.getProtocol()) && r.getDestIp() == null) {
                Range r1 = new Range(r.getStartPort(), r.getEndPort());
                Range r2 = new Range(startPort, startPort);
                if (r1.isOverlap(r2)) {
                    return;
                }
            }
        }

        logger.warn(String.format("cannot find rule[start port:%s, protocol:%s, destIp == null]", startPort, protocol));
        Assert.fail();
    }

    private void noRule(List<ApplianceVmFirewallRuleTO> tos, int startPort, ApplianceVmFirewallProtocol protocol) {
        for (ApplianceVmFirewallRuleTO r : tos) {
            if (r.getStartPort() == startPort && protocol.toString().equals(r.getProtocol())) {
                logger.warn(String.format("find unwanted rule[start port:%s, protocol:%s]", startPort, protocol));
                Assert.fail();
            }
        }
    }

    @Test
    public void test() throws ApiSenderException {
        ImageInventory iminv = deployer.images.get("TestImage");
        InstanceOfferingInventory ioinv = deployer.instanceOfferings.get("TestInstanceOffering");
        final L3NetworkInventory l3inv = deployer.l3Networks.get("TestL3Network2");
        APICreateVmInstanceMsg msg = new APICreateVmInstanceMsg();
        msg.setImageUuid(iminv.getUuid());
        msg.setInstanceOfferingUuid(ioinv.getUuid());
        List<String> l3uuids = new ArrayList<String>();
        l3uuids.add(l3inv.getUuid());
        msg.setL3NetworkUuids(l3uuids);
        msg.setName("TestVm");
        msg.setSession(session);
        msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
        msg.setType(VmInstanceConstant.USER_VM_TYPE);
        ApiSender sender = api.getApiSender();
        sender.send(msg, APICreateVmInstanceEvent.class);

        final VirtualRouterVmInventory vr = VirtualRouterVmInventory.valueOf(dbf.listAll(VirtualRouterVmVO.class).get(0));
        List<ApplianceVmFirewallRuleTO> tos = CollectionUtils.transformToList(aconfig.firewallRules, new Function<ApplianceVmFirewallRuleTO, ApplianceVmFirewallRuleTO>() {
            @Override
            public ApplianceVmFirewallRuleTO call(ApplianceVmFirewallRuleTO arg) {
                return arg.getNicMac().equals(vr.getManagementNic().getMac()) ? arg : null;
            }
        });

        Assert.assertFalse(tos.isEmpty());
        noRule(tos, 22, ApplianceVmFirewallProtocol.tcp);
        hasRule(tos, 7272, ApplianceVmFirewallProtocol.tcp);

        final VmNicInventory userNic = CollectionUtils.find(vr.getVmNics(), new Function<VmNicInventory, VmNicInventory>() {
            @Override
            public VmNicInventory call(VmNicInventory arg) {
                if (arg.getL3NetworkUuid().equals(l3inv.getUuid())) {
                    return arg;
                }
                return null;
            }
        });

        tos = CollectionUtils.transformToList(aconfig.firewallRules, new Function<ApplianceVmFirewallRuleTO, ApplianceVmFirewallRuleTO>() {
            @Override
            public ApplianceVmFirewallRuleTO call(ApplianceVmFirewallRuleTO arg) {
                return arg.getNicMac().equals(userNic.getMac()) ? arg : null;
            }
        });

        hasRule(tos, 67, ApplianceVmFirewallProtocol.udp);
        hasRule(tos, 68, ApplianceVmFirewallProtocol.udp);
        hasRule(tos, 53, ApplianceVmFirewallProtocol.udp);
        noRule(tos, 22, ApplianceVmFirewallProtocol.tcp);
        noRule(tos, 7272, ApplianceVmFirewallProtocol.tcp);
    }

}
