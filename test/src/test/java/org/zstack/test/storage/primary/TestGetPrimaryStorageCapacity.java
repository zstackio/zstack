package org.zstack.test.storage.primary;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.storage.primary.APIGetPrimaryStorageCapacityReply;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Arrays;
import java.util.List;

public class TestGetPrimaryStorageCapacity {
    CLogger logger = Utils.getLogger(TestGetPrimaryStorageCapacity.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/primaryStorage/TestQueryPrimaryStorage.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        ZoneInventory zone = deployer.zones.get("TestZone");
        ClusterInventory cluster = deployer.clusters.get("cluster1");

        List<PrimaryStorageVO> psvos = dbf.listAll(PrimaryStorageVO.class);
        long total = 0;
        long avail = 0;
        for (PrimaryStorageVO psvo : psvos) {
            total += psvo.getCapacity().getTotalCapacity();
            avail += psvo.getCapacity().getAvailableCapacity();
        }

        APIGetPrimaryStorageCapacityReply reply = api.getPrimaryStorageCapacity(Arrays.asList(zone.getUuid()), null, null);
        Assert.assertEquals(total, reply.getTotalCapacity());
        Assert.assertEquals(avail, reply.getAvailableCapacity());

        reply = api.getPrimaryStorageCapacityByAll();
        Assert.assertEquals(total, reply.getTotalCapacity());
        Assert.assertEquals(avail, reply.getAvailableCapacity());

        SimpleQuery<PrimaryStorageVO> q = dbf.createQuery(PrimaryStorageVO.class);
        q.add(PrimaryStorageVO_.name, Op.EQ, "TestPrimaryStorage1");
        PrimaryStorageVO vo1 = q.find();

        reply = api.getPrimaryStorageCapacity(null, Arrays.asList(cluster.getUuid()), null);
        Assert.assertEquals(vo1.getCapacity().getTotalCapacity(), reply.getTotalCapacity());
        Assert.assertEquals(vo1.getCapacity().getAvailableCapacity(), reply.getAvailableCapacity());

        reply = api.getPrimaryStorageCapacity(null, null, Arrays.asList(vo1.getUuid()));
        Assert.assertEquals(vo1.getCapacity().getTotalCapacity(), reply.getTotalCapacity());
        Assert.assertEquals(vo1.getCapacity().getAvailableCapacity(), reply.getAvailableCapacity());
    }
}
