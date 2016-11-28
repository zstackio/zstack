package org.zstack.test.network;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.network.l3.APIGetIpAddressCapacityReply;
import org.zstack.header.network.l3.IpRangeInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

import java.util.Arrays;

/*
 * 1. create ip range with 100 capacity
 * 2. acquire ip 30 times
 * 3. get ip range using ip range uuid and l3Network uuid
 *
 * confirm total = 100, available = 70
 */
public class TestGetIpCapacity {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    int num = 30;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/network/TestAcquireIp.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException {
        L3NetworkInventory l3nw = deployer.l3Networks.get("TestL3Network1");
        IpRangeInventory ipr = deployer.ipRanges.get("TestIpRange");
        for (int i = 0; i < num; i++) {
            api.acquireIp(l3nw.getUuid());
        }

        APIGetIpAddressCapacityReply reply = api.getIpAddressCapacity(Arrays.asList(ipr.getUuid()), null, null);
        Assert.assertEquals(101, reply.getTotalCapacity());
        Assert.assertEquals(71, reply.getAvailableCapacity());

        reply = api.getIpAddressCapacity(null, Arrays.asList(l3nw.getUuid()), null);
        Assert.assertEquals(101, reply.getTotalCapacity());
        Assert.assertEquals(71, reply.getAvailableCapacity());

        ZoneInventory zone = deployer.zones.get("TestZone");
        reply = api.getIpAddressCapacity(null, null, Arrays.asList(zone.getUuid()));
        Assert.assertEquals(101, reply.getTotalCapacity());
        Assert.assertEquals(71, reply.getAvailableCapacity());

        reply = api.getIpAddressCapacityByAll();
        Assert.assertEquals(101, reply.getTotalCapacity());
        Assert.assertEquals(71, reply.getAvailableCapacity());
    }

}
