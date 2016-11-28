package org.zstack.test.virtualrouter;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.*;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.network.service.portforwarding.PortForwardingConstant;
import org.zstack.network.service.portforwarding.PortForwardingProtocolType;
import org.zstack.network.service.portforwarding.PortForwardingRuleInventory;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.identity.IdentityCreator;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import java.util.List;

/**
 * test quota
 */
public class TestPolicyForPortForwarding1 {
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
        deployer = new Deployer("deployerXml/virtualRouter/TestPolicyForPortForwarding.xml", con);
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("VirtualRouterSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("vip.xml");
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

    PortForwardingRuleInventory createPortForwarding(String l3Uuid, SessionInventory session) throws ApiSenderException {
        PortForwardingRuleInventory rule1 = new PortForwardingRuleInventory();
        VipInventory vip = api.acquireIp(l3Uuid, session);
        rule1.setName("pfRule1");
        rule1.setVipUuid(vip.getUuid());
        rule1.setVipPortStart(22);
        rule1.setVipPortEnd(100);
        rule1.setPrivatePortStart(22);
        rule1.setPrivatePortEnd(100);
        rule1.setProtocolType(PortForwardingProtocolType.TCP.toString());
        return api.createPortForwardingRuleByFullConfig(rule1, session);
    }

    @Test
    public void test() throws ApiSenderException {
        L3NetworkInventory vipNw = deployer.l3Networks.get("PublicNetwork");

        IdentityCreator identityCreator = new IdentityCreator(api);
        AccountInventory test = identityCreator.useAccount("test");
        SessionInventory session = identityCreator.getAccountSession();

        createPortForwarding(vipNw.getUuid(), session);

        List<Quota.QuotaUsage> usages = api.getQuotaUsage(test.getUuid(), session);
        Quota.QuotaUsage u = CollectionUtils.find(usages, new Function<Quota.QuotaUsage, Quota.QuotaUsage>() {
            @Override
            public Quota.QuotaUsage call(Quota.QuotaUsage arg) {
                return arg.getName().equals(PortForwardingConstant.QUOTA_PF_NUM) ? arg : null;
            }
        });
        QuotaInventory q = api.getQuota(PortForwardingConstant.QUOTA_PF_NUM, test.getUuid(), session);
        Assert.assertEquals(1, u.getUsed().longValue());
        Assert.assertEquals(q.getValue(), u.getTotal().longValue());

        api.updateQuota(test.getUuid(), PortForwardingConstant.QUOTA_PF_NUM, 1);

        boolean s = false;
        try {
            createPortForwarding(vipNw.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.QUOTA_EXCEEDING.toString().equals(e.getError().getCode())) {
                s = true;
            }
        }
        Assert.assertTrue(s);
    }
}

