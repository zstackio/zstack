package org.zstack.test.network.flat;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.network.service.flat.FlatNetworkServiceSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.data.SizeUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * 1. has 2 IPs
 * 2. use flat network provider
 * 3. create 3 VMs concurrently
 * <p>
 * for BUG: https://github.com/zxwing/premium/issues/360
 * <p>
 * confirm only one created successfully
 */
public class TestFlatNetwork1 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;
    FlatNetworkServiceSimulatorConfig fconfig;
    long totalSize = SizeUnit.GIGABYTE.toByte(100);

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/flatnetwork/TestFlatNetwork1.xml", con);
        deployer.addSpringConfig("mevocoRelated.xml");
        deployer.load();

        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(LocalStorageSimulatorConfig.class);
        fconfig = loader.getComponent(FlatNetworkServiceSimulatorConfig.class);

        Capacity c = new Capacity();
        c.total = totalSize;
        c.avail = totalSize;

        config.capacityMap.put("host1", c);
        config.capacityMap.put("host2", c);

        deployer.build();
        api = deployer.getApi();
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        final ImageInventory image = deployer.images.get("TestImage");
        final InstanceOfferingInventory ioinv = deployer.instanceOfferings.get("TestInstanceOffering");
        final L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network1");

        final List<Boolean> success = new ArrayList<Boolean>();

        int num = 3;
        final CountDownLatch latch = new CountDownLatch(num);
        for (int i = 0; i < num; i++) {
            final int finalI = i;
            new Runnable() {
                @Override
                @AsyncThread
                public void run() {
                    try {
                        VmCreator vmCreator = new VmCreator(api);
                        vmCreator.name = String.format("vm-%s", finalI);
                        vmCreator.addL3Network(l3.getUuid());
                        vmCreator.instanceOfferingUuid = ioinv.getUuid();
                        vmCreator.imageUuid = image.getUuid();
                        vmCreator.create();
                        synchronized (success) {
                            success.add(true);
                        }
                    } catch (ApiSenderException e) {
                        // pass
                    } finally {
                        latch.countDown();
                    }
                }
            }.run();
        }

        latch.await();

        // we have 2 IPs, only one VM can be created because the flat network will use one IP for DHCP server
        Assert.assertEquals(1, success.size());
    }
}
