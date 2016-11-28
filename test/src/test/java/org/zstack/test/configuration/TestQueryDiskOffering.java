package org.zstack.test.configuration;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.APIQueryDiskOfferingMsg;
import org.zstack.header.configuration.APIQueryDiskOfferingReply;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.search.QueryTestValidator;

public class TestQueryDiskOffering {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/configuration/TestQueryDiskOffering.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        DiskOfferingInventory inv = deployer.diskOfferings.values().iterator().next();
        QueryTestValidator.validateEQ(new APIQueryDiskOfferingMsg(), api, APIQueryDiskOfferingReply.class, inv);
        QueryTestValidator.validateRandomEQConjunction(new APIQueryDiskOfferingMsg(), api, APIQueryDiskOfferingReply.class, inv, 3);
    }

}
