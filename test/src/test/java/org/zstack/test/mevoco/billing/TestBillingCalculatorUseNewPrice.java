package org.zstack.test.mevoco.billing;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.billing.APICalculateAccountSpendingReply;
import org.zstack.billing.APICreateResourcePriceMsg;
import org.zstack.billing.BillingConstants;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.HostCapacityOverProvisioningManager;
import org.zstack.header.identity.AccountConstant;
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

 */
public class TestBillingCalculatorUseNewPrice {
    CLogger logger = Utils.getLogger(TestBillingCalculatorUseNewPrice.class);
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

    private void setPriceLargerThanZero() throws ApiSenderException {
        APICreateResourcePriceMsg msg = new APICreateResourcePriceMsg();
        msg.setTimeUnit("s");
        msg.setPrice(100d);
        msg.setResourceName(BillingConstants.SPENDING_CPU);
        api.createPrice(msg);

        msg = new APICreateResourcePriceMsg();
        msg.setTimeUnit("s");
        msg.setPrice(10d);
        msg.setResourceName(BillingConstants.SPENDING_MEMORY);
        msg.setResourceUnit("m");
        api.createPrice(msg);

        msg = new APICreateResourcePriceMsg();
        msg.setTimeUnit("s");
        msg.setPrice(9d);
        msg.setResourceName(BillingConstants.SPENDING_ROOT_VOLUME);
        msg.setResourceUnit("m");
        api.createPrice(msg);
    }

    private void setPriceToZero() throws ApiSenderException {
        APICreateResourcePriceMsg msg = new APICreateResourcePriceMsg();
        msg.setTimeUnit("s");
        msg.setPrice(0d);
        msg.setResourceName(BillingConstants.SPENDING_CPU);
        api.createPrice(msg);

        msg = new APICreateResourcePriceMsg();
        msg.setTimeUnit("s");
        msg.setPrice(0d);
        msg.setResourceName(BillingConstants.SPENDING_MEMORY);
        msg.setResourceUnit("m");
        api.createPrice(msg);

        msg = new APICreateResourcePriceMsg();
        msg.setTimeUnit("s");
        msg.setPrice(0d);
        msg.setResourceName(BillingConstants.SPENDING_ROOT_VOLUME);
        msg.setResourceUnit("m");
        api.createPrice(msg);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");

        api.stopVmInstance(vm.getUuid());

        setPriceLargerThanZero();

        api.startVmInstance(vm.getUuid());

        TimeUnit.SECONDS.sleep(5);

        setPriceToZero();

        final APICalculateAccountSpendingReply reply1 =
                api.calculateSpending(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID, session);

        TimeUnit.SECONDS.sleep(5);

        final APICalculateAccountSpendingReply reply2 =
                api.calculateSpending(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID, session);

        TimeUnit.SECONDS.sleep(5);
        setPriceLargerThanZero();
        TimeUnit.SECONDS.sleep(5);

        final APICalculateAccountSpendingReply reply3 =
                api.calculateSpending(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID, session);

        setPriceToZero();
        TimeUnit.SECONDS.sleep(5);

        final APICalculateAccountSpendingReply reply4 =
                api.calculateSpending(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID, session);

        TimeUnit.SECONDS.sleep(5);

        final APICalculateAccountSpendingReply reply5 =
                api.calculateSpending(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID, session);

        logger.debug(String.format("reply1:%s", reply1.getTotal()));
        logger.debug(String.format("reply2:%s", reply2.getTotal()));
        logger.debug(String.format("reply3:%s", reply3.getTotal()));
        logger.debug(String.format("reply4:%s", reply4.getTotal()));
        logger.debug(String.format("reply5:%s", reply5.getTotal()));
        Assert.assertEquals(reply1.getTotal(), reply2.getTotal(), 10d);
        Assert.assertEquals(reply4.getTotal(), reply5.getTotal(), 10d);
    }

}
