package org.zstack.test.network;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.network.l2.APIQueryL2VlanNetworkMsg;
import org.zstack.header.network.l2.APIQueryL2VlanNetworkReply;
import org.zstack.header.network.l2.L2VlanNetworkInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.search.QueryTestValidator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class TestQueryL2VlanNetwork {
    CLogger logger = Utils.getLogger(TestQueryL2VlanNetwork.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/network/TestQueryL2VlanNetwork.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws InterruptedException, ApiSenderException {
        L2VlanNetworkInventory l2inv = (L2VlanNetworkInventory) deployer.l2Networks.values().iterator().next();
        QueryTestValidator.validateEQ(new APIQueryL2VlanNetworkMsg(), api, APIQueryL2VlanNetworkReply.class, l2inv);
    }

}
