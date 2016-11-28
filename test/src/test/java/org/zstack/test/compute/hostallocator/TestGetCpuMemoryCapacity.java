package org.zstack.test.compute.hostallocator;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.APIGetCpuMemoryCapacityReply;
import org.zstack.header.allocator.HostCpuOverProvisioningManager;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.function.Function;

import java.util.Arrays;
import java.util.List;

@Deprecated
public class TestGetCpuMemoryCapacity {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    HostCpuOverProvisioningManager cpuMgr;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/hostAllocator/TestRetrieveHostCapacity.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        cpuMgr = loader.getComponent(HostCpuOverProvisioningManager.class);
    }

    @Test
    public void test() throws ApiSenderException {
        ZoneInventory zone = api.listZones(null).get(0);
        ClusterInventory cluster = api.listClusters(null).get(0);
        List<HostInventory> hosts = api.listHosts(null);
        HostInventory host = null;
        for (HostInventory h : hosts) {
            if (h.getName().equals("TestHost1")) {
                host = h;
                break;
            }
        }

        List<String> huuids = CollectionUtils.transformToList(hosts, new Function<String, HostInventory>() {
            @Override
            public String call(HostInventory arg) {
                return arg.getUuid();
            }
        });

        long totalMemory = SizeUnit.GIGABYTE.toByte(12);

        APIGetCpuMemoryCapacityReply reply = api.retrieveHostCapacity(Arrays.asList(zone.getUuid()), null, null);
        Assert.assertEquals(totalMemory, reply.getTotalMemory());
        Assert.assertEquals(totalMemory, reply.getAvailableMemory());

        reply = api.retrieveHostCapacity(null, Arrays.asList(cluster.getUuid()), null);
        Assert.assertEquals(totalMemory, reply.getTotalMemory());
        Assert.assertEquals(totalMemory, reply.getAvailableMemory());

        reply = api.retrieveHostCapacity(null, null, Arrays.asList(host.getUuid()));
        Assert.assertEquals(cpuMgr.calculateByRatio(host.getUuid(), 4), reply.getTotalCpu());
        Assert.assertEquals(SizeUnit.GIGABYTE.toByte(8), reply.getTotalMemory());
        Assert.assertEquals(cpuMgr.calculateByRatio(host.getUuid(), 4), reply.getAvailableCpu());
        Assert.assertEquals(SizeUnit.GIGABYTE.toByte(8), reply.getAvailableMemory());

        reply = api.retrieveHostCapacity(null, null, huuids);
        Assert.assertEquals(totalMemory, reply.getTotalMemory());
        Assert.assertEquals(totalMemory, reply.getAvailableMemory());

        reply = api.retrieveHostCapacityByAll();
        Assert.assertEquals(totalMemory, reply.getTotalMemory());
        Assert.assertEquals(totalMemory, reply.getAvailableMemory());
    }
}
