package org.zstack.test.image;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.image.APIQueryImageMsg;
import org.zstack.header.image.APIQueryImageReply;
import org.zstack.header.image.ImageInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.search.QueryTestValidator;

public class TestQueryImage {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/image/TestQueryImage.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws InterruptedException, ApiSenderException {
        ImageInventory inv = deployer.images.values().iterator().next();
        QueryTestValidator.validateEQ(new APIQueryImageMsg(), api, APIQueryImageReply.class, inv);
        QueryTestValidator.validateRandomEQConjunction(new APIQueryImageMsg(), api, APIQueryImageReply.class, inv, 2);
    }
}
