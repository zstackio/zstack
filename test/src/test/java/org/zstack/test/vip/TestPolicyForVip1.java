package org.zstack.test.vip;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.*;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.network.service.vip.VipConstant;
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
public class TestPolicyForVip1 {
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
        deployer = new Deployer("deployerXml/vip/TestPolicyForVip.xml", con);
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
        L3NetworkInventory pubL3 = deployer.l3Networks.get("PublicNetwork");

        IdentityCreator identityCreator = new IdentityCreator(api);
        AccountInventory test = identityCreator.useAccount("test");
        SessionInventory session = identityCreator.getAccountSession();

        api.acquireIp(pubL3.getUuid(), session);

        List<Quota.QuotaUsage> usages = api.getQuotaUsage(test.getUuid(), session);
        Quota.QuotaUsage u = CollectionUtils.find(usages, new Function<Quota.QuotaUsage, Quota.QuotaUsage>() {
            @Override
            public Quota.QuotaUsage call(Quota.QuotaUsage arg) {
                return arg.getName().equals(VipConstant.QUOTA_VIP_NUM) ? arg : null;
            }
        });
        Assert.assertNotNull(u);
        QuotaInventory q = api.getQuota(VipConstant.QUOTA_VIP_NUM, test.getUuid(), session);
        Assert.assertEquals(1, u.getUsed().longValue());
        Assert.assertEquals(q.getValue(), u.getTotal().longValue());

        api.updateQuota(test.getUuid(), VipConstant.QUOTA_VIP_NUM, 1);

        boolean s = false;
        try {
            api.acquireIp(pubL3.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.QUOTA_EXCEEDING.toString().equals(e.getError().getCode())) {
                s = true;
            }
        }
        Assert.assertTrue(s);
    }
}

