package org.zstack.test.configuration;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.APISearchDiskOfferingMsg;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.search.APISearchMessage.NOVTriple;
import org.zstack.header.search.SearchOp;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TestSearchDiskOffering {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/configuration/TestSearchDiskOffering.xml");
        deployer.addSpringConfig("SearchManager.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        TimeUnit.SECONDS.sleep(5);
        APISearchDiskOfferingMsg msg = new APISearchDiskOfferingMsg();
        NOVTriple t = new NOVTriple();
        t.setName("diskSize");
        t.setOp(SearchOp.AND_NOT_EQ.toString());
        t.setVal(String.valueOf(SizeUnit.GIGABYTE.toByte(50)));
        msg.getNameOpValueTriples().add(t);
        String content = api.search(msg);
        List<DiskOfferingInventory> invs = JSONObjectUtil.toCollection(content, ArrayList.class, DiskOfferingInventory.class);
        DiskOfferingInventory dinv = invs.get(0);
        Assert.assertEquals(SizeUnit.GIGABYTE.toByte(120), dinv.getDiskSize());
    }

}
