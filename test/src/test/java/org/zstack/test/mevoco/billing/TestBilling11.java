package org.zstack.test.mevoco.billing;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.billing.APICalculateAccountSpendingReply;
import org.zstack.billing.APICreateResourcePriceMsg;
import org.zstack.billing.BillingConstants;
import org.zstack.billing.DataVolumeUsageVO;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.HostCapacityOverProvisioningManager;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
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
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.io.IOException;

/**
 * 1. test data volume billing
 */
public class TestBilling11 {
    CLogger logger = Utils.getLogger(TestBilling11.class);
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
        deployer = new Deployer("deployerXml/OnlyOneZone.xml", con);
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

    private void closeBillingLog() {
        // the log is too huge, we have to close it
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig("org.zstack.billing");
        loggerConfig.setLevel(Level.DEBUG);
        ctx.updateLoggers();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException, IOException {
        closeBillingLog();
        File dataFile = PathUtil.findFileOnClassPath("data/billing/TestBilling11");
        String rawData = FileUtils.readFileToString(dataFile);

        APICreateResourcePriceMsg msg = new APICreateResourcePriceMsg();
        msg.setTimeUnit("s");
        msg.setPrice(10f);
        msg.setResourceName(BillingConstants.SPENDING_TYPE_DATA_VOLUME);
        msg.setResourceUnit("m");
        api.createPrice(msg);

        for (String line : rawData.split("\n")) {
            String[] pairs = line.split(",");
            String volUuid = pairs[0].trim();
            Long date = Long.valueOf(pairs[1].trim());
            String status = pairs[2].trim();

            DataVolumeUsageVO co = new DataVolumeUsageVO();
            co.setDateInLong(date);
            co.setVolumeUuid(volUuid);
            co.setVolumeName("vol");
            co.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
            co.setVolumeSize(SizeUnit.GIGABYTE.toByte(4));
            co.setVolumeStatus(status);
            dbf.persist(co);
        }

        final APICalculateAccountSpendingReply reply = api.calculateSpending(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID,
                null, Long.MAX_VALUE, null);
        Assert.assertTrue(reply.getTotal() > 0);
    }
}
