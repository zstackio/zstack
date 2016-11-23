package org.zstack.test.mevoco.billing;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.billing.*;
import org.zstack.cassandra.CassandraFacade;
import org.zstack.cassandra.CassandraOperator;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.db.UpdateQuery;
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
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * R: running
 * S: stopped
 */
public class TestBilling3 {
    CLogger logger = Utils.getLogger(TestBilling3.class);
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

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/billing/TestBilling3.xml", con);
        deployer.addSpringConfig("mevocoRelated.xml");
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

        Capacity c = new Capacity();
        c.total = totalSize;
        c.avail = totalSize;

        config.capacityMap.put("host1", c);

        deployer.build();
        api = deployer.getApi();
        session = api.loginAsAdmin();
    }

    private void check(APICalculateAccountSpendingReply reply, double cpuPrice, double memPrice) {
        Spending spending = CollectionUtils.find(reply.getSpending(), new Function<Spending, Spending>() {
            @Override
            public Spending call(Spending arg) {
                return BillingConstants.SPENDING_TYPE_VM.equals(arg.getSpendingType()) ? arg : null;
            }
        });
        Assert.assertNotNull(spending);

        VmSpending vmSpending = (VmSpending) spending.getDetails().get(0);
        double cpuSpending = (double) vmSpending.cpuInventory.stream().mapToDouble(i -> i.spending).sum();
        Assert.assertEquals(cpuPrice, cpuSpending, 0.02);

        double memSpending = (double) vmSpending.memoryInventory.stream().mapToDouble(i -> i.spending).sum();
        Assert.assertEquals(memPrice, memSpending, 0.02);

        Assert.assertEquals(cpuPrice, cpuSpending, 0.02);
        Assert.assertEquals(memPrice, memSpending, 0.02);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        final VmInstanceInventory vm = deployer.vms.get("TestVm");
        api.stopVmInstance(vm.getUuid());
        final VmInstanceInventory vm1 = deployer.vms.get("TestVm1");
        api.stopVmInstance(vm1.getUuid());

        double cprice = 100.01d;
        double mprice = 10.03d;

        APICreateResourcePriceMsg msg = new APICreateResourcePriceMsg();
        msg.setTimeUnit("s");
        msg.setPrice(cprice);
        msg.setResourceName(BillingConstants.SPENDING_CPU);
        api.createPrice(msg);

        msg = new APICreateResourcePriceMsg();
        msg.setTimeUnit("s");
        msg.setPrice(mprice);
        msg.setResourceName(BillingConstants.SPENDING_MEMORY);
        msg.setResourceUnit("m");
        api.createPrice(msg);

        UpdateQuery uq = UpdateQuery.New();
        uq.entity(VmUsageVO.class);
        uq.condAnd(VmUsageVO_.accountUuid, Op.EQ, AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
        uq.delete();

        class CreatePrice {
            VmInstanceInventory vmInstance;

            public CreatePrice(VmInstanceInventory vmInstance) {
                this.vmInstance = vmInstance;
            }

            void create(VmInstanceState state, Date date) {
                VmUsageVO u = new VmUsageVO();
                u.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
                u.setVmUuid(vmInstance.getUuid());
                u.setCpuNum(vmInstance.getCpuNum());
                u.setMemorySize(vmInstance.getMemorySize());
                u.setInventory(JSONObjectUtil.toJsonString(vmInstance));
                u.setDateInLong(date.getTime());
                u.setName(vmInstance.getName());
                u.setState(state.toString());
                dbf.persist(u);
            }
        }

        // state: R -> S -> R -> S -> R -> S
        Date baseDate = new Date();
        CreatePrice cp = new CreatePrice(vm);
        Date date1 = new Date(baseDate.getTime() + TimeUnit.DAYS.toMillis(1));
        cp.create(VmInstanceState.Running, date1);
        Date date2 = new Date(date1.getTime() + TimeUnit.DAYS.toMillis(2));
        cp.create(VmInstanceState.Stopped, date2);
        Date date3 = new Date(date2.getTime() + TimeUnit.DAYS.toMillis(6));
        cp.create(VmInstanceState.Running, date3);
        Date date4 = new Date(date3.getTime() + TimeUnit.DAYS.toMillis(2));
        cp.create(VmInstanceState.Stopped, date4);
        Date date5 = new Date(date4.getTime() + TimeUnit.DAYS.toMillis(10));
        cp.create(VmInstanceState.Running, date5);
        Date date6 = new Date(date5.getTime() + TimeUnit.DAYS.toMillis(7));
        cp.create(VmInstanceState.Stopped, date6);

        long during1 = date2.getTime() - date1.getTime();
        long during2 = date4.getTime() - date3.getTime();
        long during3 = date6.getTime() - date5.getTime();
        long duringInSeconds = TimeUnit.MILLISECONDS.toSeconds(during1 + during2 + during3);

        logger.debug(String.format("expected seconds[during1: %s, during2: %s, during3: %s total: %s]",
                TimeUnit.MILLISECONDS.toSeconds(during1),
                TimeUnit.MILLISECONDS.toSeconds(during2),
                TimeUnit.MILLISECONDS.toSeconds(during3),
                duringInSeconds));

        APICalculateAccountSpendingReply reply = api.calculateSpending(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID, null,
                Long.MAX_VALUE, null);

        double cpuPrice = vm.getCpuNum() * cprice * duringInSeconds;
        double memPrice = (double) SizeUnit.BYTE.toMegaByte(vm.getMemorySize()) * mprice * (double) duringInSeconds;
        Assert.assertEquals(reply.getTotal(), cpuPrice + memPrice, 0.02);
        check(reply, cpuPrice, memPrice);

        baseDate = new Date(date6.getTime() + TimeUnit.DAYS.toSeconds(10));
        // S -> S -> S -> R -> R -> R -> S
        cp = new CreatePrice(vm1);
        Date date11 = new Date(baseDate.getTime() + TimeUnit.DAYS.toMillis(1));
        cp.create(VmInstanceState.Stopped, date11);
        Date date22 = new Date(date11.getTime() + TimeUnit.DAYS.toMillis(2));
        cp.create(VmInstanceState.Stopped, date22);
        Date date33 = new Date(date22.getTime() + TimeUnit.DAYS.toMillis(6));
        cp.create(VmInstanceState.Running, date33);
        Date date44 = new Date(date33.getTime() + TimeUnit.DAYS.toMillis(2));
        cp.create(VmInstanceState.Running, date44);
        Date date55 = new Date(date44.getTime() + TimeUnit.DAYS.toMillis(10));
        cp.create(VmInstanceState.Running, date55);
        Date date66 = new Date(date55.getTime() + TimeUnit.DAYS.toMillis(7));
        cp.create(VmInstanceState.Stopped, date66);
        long during11 = date66.getTime() - date33.getTime();
        duringInSeconds = TimeUnit.MILLISECONDS.toSeconds(during11);

        double cpuPrice11 = vm1.getCpuNum() * cprice * duringInSeconds;
        double memPrice11 = SizeUnit.BYTE.toMegaByte(vm1.getMemorySize()) * mprice * duringInSeconds;

        reply = api.calculateSpending(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID, date33.getTime(), date66.getTime(), null);
        Assert.assertEquals(cpuPrice11 + memPrice11, reply.getTotal(), 0.02);
        check(reply, cpuPrice11, memPrice11);
    }
}
