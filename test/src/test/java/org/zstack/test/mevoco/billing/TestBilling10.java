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

import java.util.Date;

/**
 * 1. start two vms and run for a while
 * 2. create prices with date = 1970
 * <p>
 * confirm the billing is correct
 * <p>
 * 3. delete the prices
 * <p>
 * confirm the billing is zero
 */
@Deprecated
public class TestBilling10 {
    CLogger logger = Utils.getLogger(TestBilling10.class);
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

    String rawData = "a000f4b61ad648d1bb69d6c227f246c7,1464236991107,Running\n" +
            "a000f4b61ad648d1bb69d6c227f246c7,1464296814355,Stopped\n" +
            "33aebe86b4174f70816ff1b9bd789f91,1464317634971,Stopped\n" +
            "a000f4b61ad648d1bb69d6c227f246c7,1464321394801,Running\n" +
            "85ccc136e2f34fcfbeb541575f12183f,1464328372972,Running\n" +
            "85ccc136e2f34fcfbeb541575f12183f,1464328400986,Running\n" +
            "9580766773e740669290d5638c6986f6,1464335050673,Running\n" +
            "05a830eb9e8b4d35ad6bdec4c93ccbfb,1464335541305,Running\n" +
            "05a830eb9e8b4d35ad6bdec4c93ccbfb,1464382874487,Stopped\n" +
            "16d28731f2cc411e902d83ade65a2cad,1464383013641,Stopped\n" +
            "9580766773e740669290d5638c6986f6,1464383156507,Stopped\n" +
            "a000f4b61ad648d1bb69d6c227f246c7,1464383162535,Stopped\n" +
            "85ccc136e2f34fcfbeb541575f12183f,1464400416692,Stopped\n" +
            "a2853cd537df4e2293a64ab847c28900,1464400791506,Running\n" +
            "a2853cd537df4e2293a64ab847c28900,1464400805343,Stopped\n" +
            "6815e67f921a4e67aca4cadc86dc8cbc,1464402291128,Destroyed\n" +
            "f8fa6a260d8d44f791312b08f2008b6d,1464403134351,Stopped\n" +
            "a2853cd537df4e2293a64ab847c28900,1464406241453,Running\n" +
            "85ccc136e2f34fcfbeb541575f12183f,1464409601940,Running\n" +
            "f8fa6a260d8d44f791312b08f2008b6d,1464412319672,Running\n" +
            "9683ab3ba79e4bfda8c5e111f8f1cc43,1464413677549,Stopped\n" +
            "9683ab3ba79e4bfda8c5e111f8f1cc43,1464413684584,Destroyed\n" +
            "858fa9ed9fc3431eb0a2d63df2f3bc01,1464413812636,Running\n" +
            "858fa9ed9fc3431eb0a2d63df2f3bc01,1464413828525,Stopped\n" +
            "858fa9ed9fc3431eb0a2d63df2f3bc01,1464413879223,Running\n" +
            "858fa9ed9fc3431eb0a2d63df2f3bc01,1464421965887,Stopped\n" +
            "858fa9ed9fc3431eb0a2d63df2f3bc01,1464422251124,Running\n" +
            "858fa9ed9fc3431eb0a2d63df2f3bc01,1464422271401,Stopped\n" +
            "858fa9ed9fc3431eb0a2d63df2f3bc01,1464424177567,Running\n" +
            "33aebe86b4174f70816ff1b9bd789f91,1464448324062,Running\n" +
            "33aebe86b4174f70816ff1b9bd789f91,1464448925525,Stopped\n" +
            "33aebe86b4174f70816ff1b9bd789f91,1464449309247,Running\n" +
            "33aebe86b4174f70816ff1b9bd789f91,1464469642002,Stopped\n" +
            "858fa9ed9fc3431eb0a2d63df2f3bc01,1464469860766,Stopped\n" +
            "a2853cd537df4e2293a64ab847c28900,1464469924423,Stopped\n" +
            "858fa9ed9fc3431eb0a2d63df2f3bc01,1464492046045,Running\n" +
            "a2853cd537df4e2293a64ab847c28900,1464492048560,Running\n" +
            "33aebe86b4174f70816ff1b9bd789f91,1464492061571,Running\n" +
            "33aebe86b4174f70816ff1b9bd789f91,1464555744486,Stopped\n" +
            "858fa9ed9fc3431eb0a2d63df2f3bc01,1464555854320,Stopped\n" +
            "a2853cd537df4e2293a64ab847c28900,1464555932371,Stopped\n" +
            "0b03c55b883d4130ba8161fad661bcfd,1464642497161,Stopped\n" +
            "0b03c55b883d4130ba8161fad661bcfd,1464642502669,Running\n" +
            "2315a5f4552c483eb465cca1f678648a,1464642587994,Stopped\n" +
            "2315a5f4552c483eb465cca1f678648a,1464642591969,Running\n" +
            "519a4024969c44e28fb4f64ea61190fa,1464642812747,Stopped\n" +
            "519a4024969c44e28fb4f64ea61190fa,1464642816972,Running\n" +
            "5aeade9e0254432dbaf2a29e3d473658,1464642828570,Stopped\n" +
            "5aeade9e0254432dbaf2a29e3d473658,1464642833145,Running\n" +
            "5b4c1e59b2fc4dd5806becb53ba83869,1464642841705,Stopped\n" +
            "5b4c1e59b2fc4dd5806becb53ba83869,1464642845949,Running\n" +
            "6d0d116ce5554c638896153a6ab9d080,1464643092448,Stopped\n" +
            "6d0d116ce5554c638896153a6ab9d080,1464643096697,Running\n" +
            "85ccc136e2f34fcfbeb541575f12183f,1464643226026,Stopped\n" +
            "85ccc136e2f34fcfbeb541575f12183f,1464643230040,Running\n" +
            "9a26177488ef4cdb90a5f5aa74ef25f2,1464643361242,Stopped\n" +
            "9a26177488ef4cdb90a5f5aa74ef25f2,1464643365277,Running\n" +
            "c2b1aac824fe4f248bf60770c6d553f5,1464643500166,Stopped\n" +
            "c2b1aac824fe4f248bf60770c6d553f5,1464643504315,Running\n" +
            "c7f9d190da5644c095bfa9ce5f1458fd,1464643580558,Stopped\n" +
            "c7f9d190da5644c095bfa9ce5f1458fd,1464643584733,Running\n" +
            "c904bd1074484cabbebbbe54ec502b97,1464643590967,Stopped\n" +
            "c904bd1074484cabbebbbe54ec502b97,1464643594959,Running\n" +
            "f8fa6a260d8d44f791312b08f2008b6d,1464643800139,Stopped\n" +
            "f8fa6a260d8d44f791312b08f2008b6d,1464643804180,Running\n" +
            "e2ad6a93d68e4067a0fc482c415d875f,1464659284967,Destroyed\n" +
            "f8fa6a260d8d44f791312b08f2008b6d,1464665414272,Running\n" +
            "6d0d116ce5554c638896153a6ab9d080,1464685666900,Running\n" +
            "9a26177488ef4cdb90a5f5aa74ef25f2,1464685666900,Running\n" +
            "519a4024969c44e28fb4f64ea61190fa,1464685666902,Running\n" +
            "0b03c55b883d4130ba8161fad661bcfd,1464729279209,Stopped\n" +
            "0b03c55b883d4130ba8161fad661bcfd,1464729283409,Running\n" +
            "2315a5f4552c483eb465cca1f678648a,1464729363236,Stopped\n" +
            "2315a5f4552c483eb465cca1f678648a,1464729367336,Running\n" +
            "519a4024969c44e28fb4f64ea61190fa,1464729460189,Stopped\n" +
            "519a4024969c44e28fb4f64ea61190fa,1464729464235,Running\n" +
            "5aeade9e0254432dbaf2a29e3d473658,1464729485000,Stopped\n" +
            "5aeade9e0254432dbaf2a29e3d473658,1464729489056,Running\n" +
            "5b4c1e59b2fc4dd5806becb53ba83869,1464729499532,Stopped\n" +
            "5b4c1e59b2fc4dd5806becb53ba83869,1464729503744,Running\n" +
            "6d0d116ce5554c638896153a6ab9d080,1464729872628,Stopped\n" +
            "6d0d116ce5554c638896153a6ab9d080,1464729876629,Running\n" +
            "85ccc136e2f34fcfbeb541575f12183f,1464729965319,Stopped\n" +
            "85ccc136e2f34fcfbeb541575f12183f,1464729969243,Running\n" +
            "9a26177488ef4cdb90a5f5aa74ef25f2,1464730045705,Stopped\n" +
            "9a26177488ef4cdb90a5f5aa74ef25f2,1464730049671,Running\n" +
            "c2b1aac824fe4f248bf60770c6d553f5,1464730166306,Stopped\n" +
            "c2b1aac824fe4f248bf60770c6d553f5,1464730170813,Running\n" +
            "c7f9d190da5644c095bfa9ce5f1458fd,1464730245172,Stopped\n" +
            "c7f9d190da5644c095bfa9ce5f1458fd,1464730249372,Running\n" +
            "c904bd1074484cabbebbbe54ec502b97,1464730260895,Stopped\n" +
            "c904bd1074484cabbebbbe54ec502b97,1464730265042,Running\n" +
            "f8fa6a260d8d44f791312b08f2008b6d,1464730289038,Stopped\n" +
            "f8fa6a260d8d44f791312b08f2008b6d,1464730293014,Running\n" +
            "05a830eb9e8b4d35ad6bdec4c93ccbfb,1464779013294,Running\n" +
            "16d28731f2cc411e902d83ade65a2cad,1464779022459,Running\n" +
            "05a830eb9e8b4d35ad6bdec4c93ccbfb,1464779023670,Stopped\n" +
            "16d28731f2cc411e902d83ade65a2cad,1464814925288,Stopped\n" +
            "16d28731f2cc411e902d83ade65a2cad,1464832165092,Running\n" +
            "9a26177488ef4cdb90a5f5aa74ef25f2,1464865652916,Running\n" +
            "519a4024969c44e28fb4f64ea61190fa,1464865652917,Running\n" +
            "6d0d116ce5554c638896153a6ab9d080,1464865652917,Running\n" +
            "16d28731f2cc411e902d83ade65a2cad,1464901325469,Stopped\n" +
            "16d28731f2cc411e902d83ade65a2cad,1464917795395,Running\n" +
            "16d28731f2cc411e902d83ade65a2cad,1464987725132,Stopped\n" +
            "16d28731f2cc411e902d83ade65a2cad,1465006024654,Running\n" +
            "f8fa6a260d8d44f791312b08f2008b6d,1465008181828,Stopped\n" +
            "16d28731f2cc411e902d83ade65a2cad,1465010301553,Stopped\n" +
            "16d28731f2cc411e902d83ade65a2cad,1465010308470,Running\n" +
            "0b03c55b883d4130ba8161fad661bcfd,1465011449291,Stopped\n" +
            "16d28731f2cc411e902d83ade65a2cad,1465012613315,Stopped\n" +
            "16d28731f2cc411e902d83ade65a2cad,1465012621182,Running\n" +
            "16d28731f2cc411e902d83ade65a2cad,1465012951708,Stopped\n" +
            "16d28731f2cc411e902d83ade65a2cad,1465013326354,Running\n" +
            "16d28731f2cc411e902d83ade65a2cad,1465074126500,Stopped\n" +
            "16d28731f2cc411e902d83ade65a2cad,1465124588559,Running\n" +
            "33aebe86b4174f70816ff1b9bd789f91,1465197819584,Running\n" +
            "33aebe86b4174f70816ff1b9bd789f91,1465246938340,Stopped\n" +
            "c7f9d190da5644c095bfa9ce5f1458fd,1465269873398,Running\n" +
            "c904bd1074484cabbebbbe54ec502b97,1465362992618,Stopped\n" +
            "5aeade9e0254432dbaf2a29e3d473658,1465362992634,Stopped\n" +
            "2315a5f4552c483eb465cca1f678648a,1465362992635,Stopped\n" +
            "5b4c1e59b2fc4dd5806becb53ba83869,1465362992635,Stopped\n" +
            "c2b1aac824fe4f248bf60770c6d553f5,1465362999914,Stopped\n" +
            "c7f9d190da5644c095bfa9ce5f1458fd,1465363011516,Stopped\n" +
            "6d0d116ce5554c638896153a6ab9d080,1465363014302,Stopped\n" +
            "9a26177488ef4cdb90a5f5aa74ef25f2,1465363014303,Stopped\n" +
            "519a4024969c44e28fb4f64ea61190fa,1465363014304,Stopped\n" +
            "5b4c1e59b2fc4dd5806becb53ba83869,1465363054714,Running\n" +
            "c904bd1074484cabbebbbe54ec502b97,1465363054714,Running\n" +
            "6d0d116ce5554c638896153a6ab9d080,1465363076004,Running\n" +
            "519a4024969c44e28fb4f64ea61190fa,1465363076180,Running\n" +
            "9a26177488ef4cdb90a5f5aa74ef25f2,1465363076195,Running\n" +
            "5aeade9e0254432dbaf2a29e3d473658,1465363130643,Running\n" +
            "c2b1aac824fe4f248bf60770c6d553f5,1465363130667,Running\n" +
            "c7f9d190da5644c095bfa9ce5f1458fd,1465363130792,Running\n" +
            "2315a5f4552c483eb465cca1f678648a,1465363130883,Running\n" +
            "16d28731f2cc411e902d83ade65a2cad,1465363738913,Running\n" +
            "85ccc136e2f34fcfbeb541575f12183f,1465787748690,Running\n" +
            "9e7ee8b2e6064a80add8d9f3767d8bf4,1465787841451,Destroyed\n" +
            "16d28731f2cc411e902d83ade65a2cad,1465806044399,Destroyed\n" +
            "0b03c55b883d4130ba8161fad661bcfd,1465806696880,Running\n" +
            "1a84b76a406f4d0db7e6a16d95db52b9,1465807947058,Destroyed\n" +
            "9580766773e740669290d5638c6986f6,1465807947112,Destroyed\n" +
            "05a830eb9e8b4d35ad6bdec4c93ccbfb,1465807948283,Destroyed\n" +
            "a000f4b61ad648d1bb69d6c227f246c7,1465816102862,Running\n" +
            "0b03c55b883d4130ba8161fad661bcfd,1465851788313,Stopped\n" +
            "a000f4b61ad648d1bb69d6c227f246c7,1465852051068,Stopped\n" +
            "519a4024969c44e28fb4f64ea61190fa,1465855435627,Running\n" +
            "5aeade9e0254432dbaf2a29e3d473658,1465855435627,Running\n" +
            "5b4c1e59b2fc4dd5806becb53ba83869,1465855435630,Running\n" +
            "2315a5f4552c483eb465cca1f678648a,1465855435643,Running\n" +
            "c7f9d190da5644c095bfa9ce5f1458fd,1465855435658,Running\n" +
            "de700de3bbf244ed9215d18ca973ca75,1465855435669,Running\n" +
            "9a26177488ef4cdb90a5f5aa74ef25f2,1465855435680,Running\n" +
            "c904bd1074484cabbebbbe54ec502b97,1465855435725,Running\n" +
            "9a26177488ef4cdb90a5f5aa74ef25f2,1465855496024,Running\n" +
            "c7f9d190da5644c095bfa9ce5f1458fd,1465855496156,Running\n" +
            "5b4c1e59b2fc4dd5806becb53ba83869,1465855496226,Running\n" +
            "de700de3bbf244ed9215d18ca973ca75,1465855496301,Running\n" +
            "5aeade9e0254432dbaf2a29e3d473658,1465855496365,Running\n" +
            "c904bd1074484cabbebbbe54ec502b97,1465855496387,Running\n" +
            "2315a5f4552c483eb465cca1f678648a,1465855496699,Running\n" +
            "519a4024969c44e28fb4f64ea61190fa,1465855497424,Running\n" +
            "5b4c1e59b2fc4dd5806becb53ba83869,1465855555928,Running\n" +
            "c7f9d190da5644c095bfa9ce5f1458fd,1465855555957,Running\n" +
            "c904bd1074484cabbebbbe54ec502b97,1465855556073,Running\n" +
            "de700de3bbf244ed9215d18ca973ca75,1465855556253,Running\n" +
            "2315a5f4552c483eb465cca1f678648a,1465855556279,Running\n" +
            "9a26177488ef4cdb90a5f5aa74ef25f2,1465855975930,Running\n" +
            "c7f9d190da5644c095bfa9ce5f1458fd,1465856095783,Running\n" +
            "de700de3bbf244ed9215d18ca973ca75,1465856095875,Running\n" +
            "5aeade9e0254432dbaf2a29e3d473658,1465869610129,Running\n" +
            "2315a5f4552c483eb465cca1f678648a,1465869610321,Running\n" +
            "5b4c1e59b2fc4dd5806becb53ba83869,1465869610923,Running\n" +
            "c904bd1074484cabbebbbe54ec502b97,1465869611047,Running\n" +
            "519a4024969c44e28fb4f64ea61190fa,1465869611244,Running\n";

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/billing/TestBilling10.xml", con);
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
        class CreatePrice {
            void create(String vmUuid, String vmState, long date) {
                VmUsageVO u = new VmUsageVO();
                u.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
                u.setVmUuid(vmUuid);
                u.setCpuNum(4);
                u.setRootVolumeSize(SizeUnit.GIGABYTE.toByte(100));
                u.setMemorySize(SizeUnit.GIGABYTE.toByte(4));
                u.setInventory("");
                u.setName(vmUuid);
                u.setDateInLong(date);
                u.setState(vmState);
                dbf.persist(u);
            }
        }

        for (String data : rawData.split("\n")) {
            String[] pairs = data.split(",");
            String vmUuid = pairs[0];
            String date = pairs[1];
            String state = pairs[2];

            CreatePrice createPrice = new CreatePrice();
            createPrice.create(vmUuid, state, Long.valueOf(date));
        }

        long date1970 = new Date(0).getTime();

        APICreateResourcePriceMsg msg = new APICreateResourcePriceMsg();
        msg.setTimeUnit("s");
        msg.setPrice(100f);
        msg.setResourceName(BillingConstants.SPENDING_CPU);
        msg.setDateInLong(date1970);
        PriceInventory cp = api.createPrice(msg);

        msg = new APICreateResourcePriceMsg();
        msg.setTimeUnit("s");
        msg.setPrice(10f);
        msg.setResourceName(BillingConstants.SPENDING_MEMORY);
        msg.setResourceUnit("m");
        msg.setDateInLong(date1970);
        PriceInventory mp = api.createPrice(msg);

        msg = new APICreateResourcePriceMsg();
        msg.setTimeUnit("s");
        msg.setPrice(9f);
        msg.setResourceName(BillingConstants.SPENDING_ROOT_VOLUME);
        msg.setResourceUnit("m");
        msg.setDateInLong(date1970);
        PriceInventory vp = api.createPrice(msg);

        APICalculateAccountSpendingReply reply = api.calculateSpending(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID, null);
        Assert.assertTrue(reply.getTotal() > 0);
    }
}
