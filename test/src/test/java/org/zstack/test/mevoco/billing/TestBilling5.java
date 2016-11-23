package org.zstack.test.mevoco.billing;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.billing.*;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.HostCapacityOverProvisioningManager;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;
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
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 1. test data volume billing
 */
public class TestBilling5 {
    CLogger logger = Utils.getLogger(TestBilling5.class);
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
        deployer = new Deployer("deployerXml/billing/TestBilling5.xml", con);
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
        DiskOfferingInventory doffering = deployer.diskOfferings.get("DataDiskOffering");
        VolumeInventory vol = api.createDataVolume("data", doffering.getUuid());

        APICreateResourcePriceMsg msg = new APICreateResourcePriceMsg();
        msg.setTimeUnit("s");
        msg.setPrice(10d);
        msg.setResourceName(BillingConstants.SPENDING_TYPE_DATA_VOLUME);
        msg.setResourceUnit("m");
        api.createPrice(msg);

        VmInstanceInventory vm = deployer.vms.get("TestVm");

        vol = api.attachVolumeToVm(vm.getUuid(), vol.getUuid());

        TimeUnit.SECONDS.sleep(5);

        api.deleteDataVolume(vol.getUuid());
        VolumeVO volvo = dbf.findByUuid(vol.getUuid(), VolumeVO.class);
        long during = TimeUnit.MILLISECONDS.toSeconds(volvo.getLastOpDate().getTime() - vol.getLastOpDate().getTime());

        logger.debug(String.format("duration: %s s", during));

        long volSize = SizeUnit.BYTE.toMegaByte(vol.getSize());
        double price = 10 * volSize * during;

        double errorMargin = 10 * volSize * 2; // the error margin of duration is 2s

        final APICalculateAccountSpendingReply reply = api.calculateSpending(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID,
                null, Long.MAX_VALUE, null);

        Assert.assertEquals(price, reply.getTotal(), errorMargin);

        Optional<Spending> opt = reply.getSpending().stream()
                .filter(s -> s.getSpendingType().equals(BillingConstants.SPENDING_TYPE_DATA_VOLUME)).findFirst();
        Assert.assertTrue(opt.isPresent());
        Spending spending = opt.get();
        Assert.assertEquals(1, spending.getDetails().size());
        SpendingDetails sp = spending.getDetails().get(0);
        DataVolumeSpending ds = JSONObjectUtil.rehashObject(sp, DataVolumeSpending.class);
        Assert.assertEquals(volvo.getUuid(), ds.resourceUuid);
        Assert.assertFalse(ds.sizeInventory.isEmpty());

        double volPrice = (double) ds.sizeInventory.stream().mapToDouble(i -> i.spending).sum();
        Assert.assertEquals(price, volPrice, errorMargin);
    }
}
