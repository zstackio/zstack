package org.zstack.test.mevoco.billing;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.billing.*;
import org.zstack.cassandra.APIQueryCassandraReply;
import org.zstack.cassandra.CassandraFacade;
import org.zstack.cassandra.CassandraOperator;
import org.zstack.cassandra.Cql;
import org.zstack.cassandra.CqlQuery.Op;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.HostCapacityOverProvisioningManager;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.network.service.flat.FlatNetworkServiceSimulatorConfig;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

/**
 * test query price
 */
public class TestBilling6 {
    CLogger logger = Utils.getLogger(TestBilling6.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;
    FlatNetworkServiceSimulatorConfig fconfig;
    KVMSimulatorConfig kconfig;
    PrimaryStorageOverProvisioningManager psRatioMgr;
    HostCapacityOverProvisioningManager hostRatioMgr;
    long totalSize = SizeUnit.GIGABYTE.toByte(100);
    CassandraFacade cassf;
    CassandraOperator ops;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        DBUtil.reDeployCassandra(BillingConstants.CASSANDRA_KEYSPACE);
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/mevoco/TestMevoco.xml", con);
        deployer.addSpringConfig("mevocoRelated.xml");
        deployer.addSpringConfig("cassandra.xml");
        deployer.addSpringConfig("billing.xml");
        deployer.load();

        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(LocalStorageSimulatorConfig.class);
        fconfig = loader.getComponent(FlatNetworkServiceSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        psRatioMgr = loader.getComponent(PrimaryStorageOverProvisioningManager.class);
        hostRatioMgr = loader.getComponent(HostCapacityOverProvisioningManager.class);
        cassf = loader.getComponent(CassandraFacade.class);
        ops = cassf.getOperator(BillingConstants.CASSANDRA_KEYSPACE);

        Capacity c = new Capacity();
        c.total = totalSize;
        c.avail = totalSize;

        config.capacityMap.put("host1", c);

        deployer.build();
        api = deployer.getApi();
        session = api.loginAsAdmin();
    }
    
	@Test
	public void test() throws ApiSenderException, InterruptedException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        api.stopVmInstance(vm.getUuid());

        TimeUnit.SECONDS.sleep(1);

        APICreateResourcePriceMsg msg = new APICreateResourcePriceMsg();
        msg.setTimeUnit("s");
        msg.setPrice(100f);
        msg.setResourceName(BillingConstants.SPENDING_CPU);
        api.createPrice(msg);
        Cql cql = new Cql("select * from <table> where resourceName = :name limit 1");
        cql.setTable(PriceCO.class.getSimpleName()).setParameter("name", BillingConstants.SPENDING_CPU);
        PriceCO co = ops.selectOne(cql.build(), PriceCO.class);
        Assert.assertNotNull(co);

        msg = new APICreateResourcePriceMsg();
        msg.setTimeUnit("s");
        msg.setPrice(10f);
        msg.setResourceName(BillingConstants.SPENDING_MEMORY);
        msg.setResourceUnit("m");
        api.createPrice(msg);

        msg = new APICreateResourcePriceMsg();
        msg.setTimeUnit("s");
        msg.setPrice(9f);
        msg.setResourceName(BillingConstants.SPENDING_ROOT_VOLUME);
        msg.setResourceUnit("m");
        api.createPrice(msg);

        APIQueryResourcePriceMsg qmsg = new APIQueryResourcePriceMsg();
        qmsg.setResourceName(BillingConstants.SPENDING_CPU);
        APIQueryResourcePriceReply reply = api.queryCassandra(qmsg, APIQueryResourcePriceReply.class);
        Assert.assertEquals(1, reply.getInventories().size());
        PriceInventory inv = reply.getInventories().get(0);
        Assert.assertEquals(100f, inv.getPrice(), 0);

        qmsg = new APIQueryResourcePriceMsg();
        qmsg.setCount(true);
        qmsg.setResourceName(BillingConstants.SPENDING_MEMORY);
        APIQueryCassandraReply cr = api.queryCassandra(qmsg, APIQueryCassandraReply.class);
        Assert.assertEquals(1, cr.getTotal().intValue());

        qmsg = new APIQueryResourcePriceMsg();
        qmsg.setReplyWithCount(true);
        qmsg.setResourceName(BillingConstants.SPENDING_ROOT_VOLUME);
        qmsg.addCondition("dateInLong", Op.LTE, String.valueOf(System.currentTimeMillis()));
        reply = api.queryCassandra(qmsg, APIQueryResourcePriceReply.class);
        Assert.assertEquals(1, reply.getTotal().intValue());
        Assert.assertEquals(1, reply.getInventories().size());
        inv = reply.getInventories().get(0);
        Assert.assertEquals(9f, inv.getPrice(), 0);
    }
}
