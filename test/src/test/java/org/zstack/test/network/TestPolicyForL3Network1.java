package org.zstack.test.network;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.IdentityErrors;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l3.IpRangeInventory;
import org.zstack.header.network.l3.L3NetworkConstant;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.identity.IdentityCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class TestPolicyForL3Network1 {
    CLogger logger = Utils.getLogger(TestPolicyForL3Network1.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/network/TestPolicyForL3Network.xml");
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        L2NetworkInventory l2 = deployer.l2Networks.get("TestL2Network");

        IdentityCreator identityCreator = new IdentityCreator(api);
        AccountInventory test = identityCreator.useAccount("test");

        SessionInventory session = identityCreator.getAccountSession();
        api.createL3BasicNetwork(l2.getUuid(), session);

        api.updateQuota(test.getUuid(), L3NetworkConstant.QUOTA_L3_NUM, 1);

        boolean success = false;
        try {
            api.createL3BasicNetwork(l2.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.QUOTA_EXCEEDING.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }

        Assert.assertTrue(success);
    }
}

