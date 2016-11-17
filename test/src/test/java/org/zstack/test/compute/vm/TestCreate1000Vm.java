package org.zstack.test.compute.vm;

import org.apache.commons.lang.time.StopWatch;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.allocator.HostAllocatorGlobalConfig;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.SyncThread;
import org.zstack.core.thread.ThreadGlobalProperty;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.simulator.storage.primary.SimulatorPrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.identity.IdentityGlobalConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestCreate1000Vm {
    CLogger logger = Utils.getLogger(TestCreate1000Vm.class);

    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    int vmNum = 1000;
    CountDownLatch latch = new CountDownLatch(vmNum);
    List<Long> timeCost = new ArrayList<Long>();
    ClusterInventory cluster;
    ZoneInventory zone;
    int hostNum = 1;
    CountDownLatch hostLatch = new CountDownLatch(hostNum);
    int psNum = 1;
    CountDownLatch psLatch = new CountDownLatch(psNum);

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/vm/CreateVm1000.xml", con);
        ThreadGlobalProperty.MAX_THREAD_NUM = 2000;
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        api.setTimeout(1200);
    }

    @SyncThread(level = 1000)
    private void createVm(VmInstanceInventory vm, String rootDiskUuid, List<String> nws, List<String> disks) throws ApiSenderException {
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            api.createVmByFullConfig(vm, rootDiskUuid, nws, disks);
        } finally {
            watch.stop();
            timeCost.add(watch.getTime());
            latch.countDown();
        }
    }

    @SyncThread(level = 50)
    private void addHost(long ip, int index) throws ApiSenderException {
        try {
            HostInventory host = new HostInventory();
            host.setName("simulator-" + index);
            host.setClusterUuid(cluster.getUuid());
            host.setManagementIp(NetworkUtils.longToIpv4String(ip));
            host.setAvailableCpuCapacity(100000L);
            host.setAvailableMemoryCapacity(SizeUnit.TERABYTE.toByte(32));
            api.addHostByFullConfig(host);
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            System.exit(1);
        } finally {
            hostLatch.countDown();
        }
    }

    private void addHosts() throws ApiSenderException, InterruptedException {
        String ip = "10.1.0.2";
        long iplong = NetworkUtils.ipv4StringToLong(ip);
        for (int i=0; i<hostNum; i++) {
            addHost(++iplong, i);
        }

        hostLatch.await(30, TimeUnit.MINUTES);
    }

    @SyncThread(level = 1000)
    private void addPrimaryStorage(int index) throws ApiSenderException {
        try {
            PrimaryStorageInventory ps = new PrimaryStorageInventory();
            ps.setName("ps-" + index);
            ps.setTotalCapacity(SizeUnit.TERABYTE.toByte(10000));
            ps.setAvailableCapacity(SizeUnit.TERABYTE.toByte(10000));
            ps.setUrl("nfs://ps-" + index);
            ps.setZoneUuid(zone.getUuid());
            ps.setType(SimulatorPrimaryStorageConstant.SIMULATOR_PRIMARY_STORAGE_TYPE);
            ps = api.addPrimaryStorageByFullConfig(ps);
            api.attachPrimaryStorage(cluster.getUuid(), ps.getUuid());
        } catch (Throwable t) {
            logger.error(t.getMessage(), t);
            System.exit(1);
        } finally {
            psLatch.countDown();
        }
    }

    private void addPrimaryStorage() throws ApiSenderException, InterruptedException {
        for (int i=0; i<psNum; i++) {
            addPrimaryStorage(i);
        }

        psLatch.await(30, TimeUnit.MINUTES);
    }
    
    @Test
    public void test() throws ApiSenderException, InterruptedException {
        HostAllocatorGlobalConfig.USE_PAGINATION.updateValue(true);
        HostAllocatorGlobalConfig.PAGINATION_LIMIT.updateValue(5);
        cluster = deployer.clusters.get("TestCluster");
        zone = deployer.zones.get("TestZone");
        CoreGlobalProperty.VM_TRACER_ON = false;
        IdentityGlobalConfig.SESSION_TIMEOUT.updateValue(TimeUnit.HOURS.toSeconds(1000));
        api.prepare();

        addHosts();
        addPrimaryStorage();

        InstanceOfferingInventory ioinv = api.listInstanceOffering(null).get(0);
        ImageInventory iminv = api.listImage(null).get(0);
        List<DiskOfferingInventory> dinvs = api.listDiskOffering(null);
        List<L3NetworkInventory> nwinvs = api.listL3Network(null);
        List<String> nws = new ArrayList<String>(nwinvs.size());
        for (L3NetworkInventory nwinv : nwinvs) {
            nws.add(nwinv.getUuid());
        }

        for (int i = 0; i < vmNum; i++) {
            VmInstanceInventory vm = new VmInstanceInventory();
            vm.setDescription("TestVm");
            vm.setName("TestVm");
            vm.setType(VmInstanceConstant.USER_VM_TYPE);
            vm.setInstanceOfferingUuid(ioinv.getUuid());
            vm.setImageUuid(iminv.getUuid());
            createVm(vm, dinvs.get(0).getUuid(), nws, new ArrayList<String>());
        }
        latch.await(600, TimeUnit.MINUTES);
        long totalTime = 0;
        long minTime = 0;
        long maxTime = 0;
        for (Long t : timeCost) {
            minTime = Math.min(minTime, t);
            maxTime = Math.max(maxTime, t);
            totalTime += t;
        }
        System.out.println(String.format("total time: %s, min time: %s, max time: %s, avg  time: %s",
                TimeUnit.MILLISECONDS.toSeconds(totalTime),
                TimeUnit.MILLISECONDS.toSeconds(minTime),
                TimeUnit.MILLISECONDS.toSeconds(maxTime),
                TimeUnit.MILLISECONDS.toSeconds(totalTime/timeCost.size())
        ));

        /*
        SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
        q.add(VmInstanceVO_.state, Op.EQ, VmInstanceState.Running);
        long count = q.count();
        Assert.assertEquals(vmNum, count);
        */
    }
}
