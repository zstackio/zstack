package org.zstack.test.configuration;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.APIQueryInstanceOfferingMsg;
import org.zstack.header.configuration.APIQueryInstanceOfferingReply;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.search.QueryTestValidator;

public class TestQueryInstanceOffering {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/configuration/TestQueryInstanceOffering.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException, JSONException {
        InstanceOfferingInventory inv = deployer.instanceOfferings.values().iterator().next();
        QueryTestValidator.validateEQ(new APIQueryInstanceOfferingMsg(), api, APIQueryInstanceOfferingReply.class, inv);
        QueryTestValidator.validateRandomEQConjunction(new APIQueryInstanceOfferingMsg(), api, APIQueryInstanceOfferingReply.class, inv, 3);
    }

}
