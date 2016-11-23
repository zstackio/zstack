package org.zstack.test.mevoco.billing;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.billing.*;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.HostCapacityOverProvisioningManager;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**

 */
public class TestBilling {
    CLogger logger = Utils.getLogger(TestBilling.class);
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
        deployer = new Deployer("deployerXml/mevoco/TestMevoco.xml", con);
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

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        BillingGlobalConfig.SAMPLING_INTERVAL.updateValue(1);

        VmInstanceInventory vm = deployer.vms.get("TestVm");
        api.stopVmInstance(vm.getUuid());

        VolumeInventory rootVolume = vm.getRootVolume();

        TimeUnit.SECONDS.sleep(1);

        APICreateResourcePriceMsg msg = new APICreateResourcePriceMsg();
        msg.setTimeUnit("s");
        msg.setPrice(100f);
        msg.setResourceName(BillingConstants.SPENDING_CPU);
        PriceInventory priceinv = api.createPrice(msg);
        Assert.assertTrue(dbf.isExist(priceinv.getUuid(), PriceVO.class));

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

        long during = 5;
        api.startVmInstance(vm.getUuid());
        TimeUnit.SECONDS.sleep(during);
        api.stopVmInstance(vm.getUuid());

        TimeUnit.SECONDS.sleep(2);

        final APICalculateAccountSpendingReply reply = api.calculateSpending(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID, null);

        float cpuPrice = vm.getCpuNum() * 100f * during;
        float memPrice = vm.getMemorySize() * 10f * during;
        long volSizeInM = SizeUnit.BYTE.toMegaByte(rootVolume.getSize());
        float volPrice = volSizeInM * 9f * during;

        // for 2s error margin
        float cpuPriceErrorMargin = vm.getCpuNum() * 100f * 2;
        float memPriceErrorMargin = vm.getMemorySize() * 100f * 2;
        float volPriceErrorMargin = volSizeInM * 9f * 2;
        float errorMargin = cpuPriceErrorMargin + memPriceErrorMargin + volPriceErrorMargin;

        Assert.assertEquals(cpuPrice + memPrice + volPrice, reply.getTotal(), errorMargin);

        Spending spending = CollectionUtils.find(reply.getSpending(), arg -> BillingConstants.SPENDING_TYPE_VM.equals(arg.getSpendingType()) ? arg : null);
        Assert.assertNotNull(spending);
        List<VmSpending> vmSpendings = JSONObjectUtil.toCollection(JSONObjectUtil.toJsonString(spending.getDetails()),
                ArrayList.class, VmSpending.class);
        VmSpending vmSpending = vmSpendings.get(0);

        float cpuSpending = (float) vmSpending.cpuInventory.stream().mapToDouble(i -> i.spending).sum();
        Assert.assertEquals(cpuPrice, cpuSpending, cpuPriceErrorMargin);

        float memSpending = (float) vmSpending.memoryInventory.stream().mapToDouble(i -> i.spending).sum();
        Assert.assertEquals(memPrice, memSpending, memPriceErrorMargin);

        spending = CollectionUtils.find(reply.getSpending(), arg -> BillingConstants.SPENDING_TYPE_ROOT_VOLUME.equals(arg.getSpendingType()) ? arg : null);
        Assert.assertNotNull(spending);
        Assert.assertEquals(volPrice, spending.getSpending(), volPriceErrorMargin);
        RootVolumeSpending rootVolumeSpending = (RootVolumeSpending) spending.getDetails().get(0);

        float rootVolSpending = (float) rootVolumeSpending.sizeInventory.stream().mapToDouble(i -> i.spending).sum();
        Assert.assertEquals(volPrice, rootVolSpending, volPriceErrorMargin);

        api.destroyVmInstance(vm.getUuid());
        APICalculateAccountSpendingReply reply1 = api.calculateSpending(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID, null);
        TimeUnit.SECONDS.sleep(3);
        APICalculateAccountSpendingReply reply2 = api.calculateSpending(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID, null);
        Assert.assertEquals(reply1.getTotal(), reply2.getTotal(), 0.2);
    }
}
