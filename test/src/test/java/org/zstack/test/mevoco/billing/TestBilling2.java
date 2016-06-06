package org.zstack.test.mevoco.billing;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.billing.*;
import org.zstack.cassandra.CassandraFacade;
import org.zstack.cassandra.CassandraOperator;
import org.zstack.cassandra.Cql;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.HostCapacityOverProvisioningManager;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.network.service.flat.FlatNetworkServiceSimulatorConfig;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * R: running
 * S: stopped
 *
 * usage state: R -> S -> S -> R -> S
 */
public class TestBilling2 {
    CLogger logger = Utils.getLogger(TestBilling2.class);
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

    private void check(APICalculateAccountSpendingReply reply, float cpuPrice, float memPrice) {
        Spending spending = CollectionUtils.find(reply.getSpending(), arg -> BillingConstants.SPENDING_TYPE_VM.equals(arg.getSpendingType()) ? arg : null);
        Assert.assertNotNull(spending);

        VmSpending vmSpending = (VmSpending) spending.getDetails().get(0);
        float cpuSpending = (float) vmSpending.cpuInventory.stream().mapToDouble(i -> i.spending).sum();
        Assert.assertEquals(cpuPrice, cpuSpending, 0.02);

        float memSpending = (float) vmSpending.memoryInventory.stream().mapToDouble(i -> i.spending).sum();
        Assert.assertEquals(memPrice, memSpending, 0.02);
    }
    
	@Test
	public void test() throws ApiSenderException, InterruptedException {
        final VmInstanceInventory vm = deployer.vms.get("TestVm");
        api.stopVmInstance(vm.getUuid());

        float cprice = 100.01f;
        float mprice = 10.03f;

        APICreateResourcePriceMsg msg = new APICreateResourcePriceMsg();
        msg.setTimeUnit("s");
        msg.setPrice(cprice);
        msg.setResourceName(BillingConstants.SPENDING_CPU);
        api.createPrice(msg);
        Cql cql = new Cql("select * from <table> where resourceName = :name limit 1");
        cql.setTable(PriceCO.class.getSimpleName()).setParameter("name", BillingConstants.SPENDING_CPU);
        PriceCO cpupco = ops.selectOne(cql.build(), PriceCO.class);
        Assert.assertNotNull(cpupco);

        msg = new APICreateResourcePriceMsg();
        msg.setTimeUnit("s");
        msg.setPrice(mprice);
        msg.setResourceName(BillingConstants.SPENDING_MEMORY);
        msg.setResourceUnit("m");
        api.createPrice(msg);
        cql = new Cql("select * from <table> where resourceName = :name limit 1");
        cql.setTable(PriceCO.class.getSimpleName()).setParameter("name", BillingConstants.SPENDING_MEMORY);
        PriceCO mempco = ops.selectOne(cql.build(), PriceCO.class);
        Assert.assertNotNull(mempco);

        cql = new Cql("delete from <table> where accountUuid = :uuid");
        cql.setTable(VmUsageCO.class.getSimpleName()).setParameter("uuid", AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
        ops.execute(cql.build());

        class CreatePrice {
            void create(VmInstanceState state, Date date) {
                VmUsageCO u = new VmUsageCO();
                u.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
                u.setVmUuid(vm.getUuid());
                u.setCpuNum(vm.getCpuNum());
                u.setMemorySize(vm.getMemorySize());
                u.setInventory(JSONObjectUtil.toJsonString(vm));
                u.setDateInLong(date.getTime());
                u.setName(vm.getName());
                u.setDate();
                u.setState(state.toString());
                ops.insert(u);
            }
        }

        Date baseDate = new Date();
        CreatePrice cp = new CreatePrice();
        Date date1 = new Date(baseDate.getTime() + TimeUnit.DAYS.toMillis(1));
        cp.create(VmInstanceState.Running, date1);
        Date date2 = new Date(date1.getTime() + TimeUnit.DAYS.toMillis(2));
        cp.create(VmInstanceState.Stopped, date2);
        Date date3 = new Date(date2.getTime() + TimeUnit.DAYS.toMillis(6));
        cp.create(VmInstanceState.Stopped, date3);
        Date date4 = new Date(date3.getTime() + TimeUnit.DAYS.toMillis(2));
        cp.create(VmInstanceState.Running, date4);
        Date date5 = new Date(date4.getTime() + TimeUnit.DAYS.toMillis(10));
        cp.create(VmInstanceState.Stopped, date5);

        long during1 = date2.getTime() - date1.getTime();
        long during2 = date5.getTime() - date4.getTime();
        long duringInSeconds = TimeUnit.MILLISECONDS.toSeconds(during1 + during2);

        logger.debug(String.format("expected seconds[during1: %s, during2: %s, total: %s]",
                TimeUnit.MILLISECONDS.toSeconds(during1), TimeUnit.MILLISECONDS.toSeconds(during2), duringInSeconds));

        APICalculateAccountSpendingReply reply = api.calculateSpending(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID, null, Long.MAX_VALUE, null);

        float cpuPrice = vm.getCpuNum() * cprice * duringInSeconds;
        float memPrice = SizeUnit.BYTE.toMegaByte(vm.getMemorySize()) * mprice * duringInSeconds;
        Assert.assertEquals(cpuPrice + memPrice, reply.getTotal(), 0.02);
        check(reply, cpuPrice, memPrice);

        // R[start point] -> S[end point]
        reply = api.calculateSpending(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID, date4.getTime(), date5.getTime(), null);
        duringInSeconds = TimeUnit.MILLISECONDS.toSeconds(during2);
        cpuPrice = vm.getCpuNum() * cprice * duringInSeconds;
        memPrice = SizeUnit.BYTE.toMegaByte(vm.getMemorySize()) * mprice * duringInSeconds;
        Assert.assertEquals(cpuPrice + memPrice, reply.getTotal(), 0.02);
        check(reply, cpuPrice, memPrice);

        // R --> start point --> S[end point]
        Date date7 = new Date(date4.getTime() + TimeUnit.DAYS.toMillis(2));
        reply = api.calculateSpending(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID, date7.getTime(), date5.getTime(), null);
        duringInSeconds = TimeUnit.MILLISECONDS.toSeconds(date5.getTime() - date7.getTime());
        cpuPrice = vm.getCpuNum() * cprice * duringInSeconds;
        memPrice = SizeUnit.BYTE.toMegaByte(vm.getMemorySize()) * mprice * duringInSeconds;
        Assert.assertEquals(cpuPrice + memPrice, reply.getTotal(), 0.02);
        check(reply, cpuPrice, memPrice);

        // R --> start point --> end point -> S
        Date date8 = new Date(date4.getTime() + TimeUnit.DAYS.toMillis(2));
        Date date9 = new Date(date4.getTime() + TimeUnit.DAYS.toMillis(4));
        reply = api.calculateSpending(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID, date8.getTime(), date9.getTime(), null);
        duringInSeconds = TimeUnit.MILLISECONDS.toSeconds(date9.getTime() - date8.getTime());
        cpuPrice = vm.getCpuNum() * cprice * duringInSeconds;
        memPrice = SizeUnit.BYTE.toMegaByte(vm.getMemorySize()) * mprice * duringInSeconds;
        Assert.assertEquals(cpuPrice + memPrice, reply.getTotal(), 0.02);
        check(reply, cpuPrice, memPrice);

        // R[start point] -> S --> end point
        Date date10 = new Date(date4.getTime() + TimeUnit.DAYS.toMillis(12));
        reply = api.calculateSpending(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID, date4.getTime(), date10.getTime(), null);
        duringInSeconds = TimeUnit.MILLISECONDS.toSeconds(date5.getTime() - date4.getTime());
        cpuPrice = vm.getCpuNum() * cprice * duringInSeconds;
        memPrice = SizeUnit.BYTE.toMegaByte(vm.getMemorySize()) * mprice * duringInSeconds;
        Assert.assertEquals(cpuPrice + memPrice, reply.getTotal(), 0.02);
        check(reply, cpuPrice, memPrice);

        // start point -> R -> S --> end point
        Date date11 = new Date(date4.getTime() - TimeUnit.DAYS.toMillis(3));
        Date date12 = new Date(date5.getTime() + TimeUnit.DAYS.toMillis(6));
        reply = api.calculateSpending(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID, date11.getTime(), date12.getTime(), null);
        duringInSeconds = TimeUnit.MILLISECONDS.toSeconds(date5.getTime() - date4.getTime());
        cpuPrice = vm.getCpuNum() * cprice * duringInSeconds;
        memPrice = SizeUnit.BYTE.toMegaByte(vm.getMemorySize()) * mprice * duringInSeconds;
        Assert.assertEquals(cpuPrice + memPrice, reply.getTotal(), 0.02);
        check(reply, cpuPrice, memPrice);
    }
}
