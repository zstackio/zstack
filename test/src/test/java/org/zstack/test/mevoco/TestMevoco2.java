package org.zstack.test.mevoco;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.network.l3.L3NetworkDnsVO;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmNicVO;
import org.zstack.network.service.flat.FlatDhcpBackend.ApplyDhcpCmd;
import org.zstack.network.service.flat.FlatDhcpBackend.DhcpInfo;
import org.zstack.network.service.flat.FlatNetworkServiceSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.function.Function;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 1. concurrently add image and create vm
 *
 * confirm 50 vms created successfully
 *
 * 2. reconnect the host
 *
 * confirm all DHCP setup for vms
 */
public class TestMevoco2 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;
    FlatNetworkServiceSimulatorConfig fconfig;
    long totalSize = SizeUnit.GIGABYTE.toByte(100);
    int num = 10;
    CountDownLatch latch = new CountDownLatch(num);
    final List<VmInstanceInventory> vms = new ArrayList<VmInstanceInventory>();

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/mevoco/TestMevoco.xml", con);
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

    private void checkNic(VmInstanceVO vm, List<DhcpInfo> info) {
        VmNicVO nic = vm.getVmNics().iterator().next();
        DhcpInfo target = null;
        for (DhcpInfo i : info) {
            if (i.mac.equals(nic.getMac())) {
                target = i;
                break;
            }
        }

        Assert.assertNotNull(target);
        Assert.assertEquals(nic.getIp(), target.ip);
        Assert.assertEquals(nic.getNetmask(), target.netmask);
        Assert.assertEquals(nic.getGateway(), target.gateway);
        Assert.assertEquals(true, target.isDefaultL3Network);
        String hostname = VmSystemTags.HOSTNAME.getTokenByResourceUuid(vm.getUuid(), VmSystemTags.HOSTNAME_TOKEN);
        if (hostname == null) {
            hostname = nic.getIp().replaceAll("\\.", "-");
        }
        Assert.assertEquals(hostname, target.hostname);
        L3NetworkVO l3 = dbf.findByUuid(nic.getL3NetworkUuid(), L3NetworkVO.class);
        Assert.assertEquals(l3.getDnsDomain(), target.dnsDomain);
        Assert.assertNotNull(target.dns);
        List<String> dns = CollectionUtils.transformToList(l3.getDns(), new Function<String, L3NetworkDnsVO>() {
            @Override
            public String call(L3NetworkDnsVO arg) {
                return arg.getDns();
            }
        });
        Assert.assertTrue(dns.containsAll(target.dns));
        Assert.assertTrue(target.dns.containsAll(dns));
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

        Assert.assertEquals(num, vms.size());

        fconfig.applyDhcpCmdList.clear();
        HostInventory host = deployer.hosts.get("host1");
        api.reconnectHost(host.getUuid());
        Assert.assertFalse(fconfig.applyDhcpCmdList.isEmpty());
        ApplyDhcpCmd cmd = fconfig.applyDhcpCmdList.get(0);
        Assert.assertEquals(num+1, cmd.dhcp.size());

        List<VmInstanceVO> vms = dbf.listAll(VmInstanceVO.class);
        for (VmInstanceVO vm : vms) {
            checkNic(vm, cmd.dhcp);
        }
    }
}
