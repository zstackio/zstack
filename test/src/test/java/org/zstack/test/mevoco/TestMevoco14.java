package org.zstack.test.mevoco;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.network.service.flat.FlatNetworkServiceSimulatorConfig;
import org.zstack.network.service.flat.FlatNetworkSystemTags;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.data.SizeUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 1. set the IP range with 49 IP
 * 2. concurrently create 50 vm
 *
 * confirm only 49 vms created successfully
 * confirm the 49 vms don't occupy the DHCP server IP
 */
public class TestMevoco14 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;
    FlatNetworkServiceSimulatorConfig fconfig;
    long totalSize = SizeUnit.GIGABYTE.toByte(100);
    int num = 50;
    CountDownLatch latch = new CountDownLatch(num);
    final List<VmInstanceInventory> vms = new ArrayList<VmInstanceInventory>();

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/mevoco/TestMevoco14.xml", con);
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

    @AsyncThread
    private void createVm(ImageInventory img, String bsUuid) throws ApiSenderException {
        img = api.addImage(img, bsUuid);
        InstanceOfferingInventory ioinv = deployer.instanceOfferings.get("small");
        L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network1");
        VmCreator creator = new VmCreator(api);
        creator.imageUuid = img.getUuid();
        creator.session = api.getAdminSession();
        creator.instanceOfferingUuid = ioinv.getUuid();
        creator.name = "vm";
        creator.addL3Network(l3.getUuid());
        try {
            synchronized (vms) {
                vms.add(creator.create());
            }
        } finally {
            latch.countDown();
        }
    }

	@Test
	public void test() throws ApiSenderException, InterruptedException {
        final BackupStorageInventory sftp = deployer.backupStorages.get("sftp");
        final ImageInventory img = new ImageInventory();
        img.setName("image");
        img.setPlatform(ImagePlatform.Linux.toString());
        img.setMediaType(ImageMediaType.RootVolumeTemplate.toString());
        img.setFormat("qcow2");
        img.setUrl("http://test.img");

        for (int i=0; i<num; i++) {
            createVm(img, sftp.getUuid());
        }

        latch.await(5, TimeUnit.MINUTES);

        Assert.assertEquals(num - 1, vms.size());

        L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network1");
        String dhcpIp = FlatNetworkSystemTags.L3_NETWORK_DHCP_IP.getTokenByResourceUuid(l3.getUuid(), FlatNetworkSystemTags.L3_NETWORK_DHCP_IP_TOKEN);
        for (VmInstanceInventory vm : vms) {
            if (vm.getVmNics().get(0).getIp().equals(dhcpIp)) {
                Assert.fail(String.format("vm[uuid:%s] uses the DHCP IP[%s]", vm.getUuid(), dhcpIp));
            }
        }
    }
}
