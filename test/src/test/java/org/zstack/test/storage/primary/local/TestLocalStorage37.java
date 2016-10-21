package org.zstack.test.storage.primary.local;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeType;
import org.zstack.storage.primary.local.LocalStorageHostRefVO;
import org.zstack.storage.primary.local.LocalStorageHostRefVOFinder;
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
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.stopwatch.StopWatch;

import java.util.List;

/**
 * 1. use local storage
 * 2. create a vm with data volume
 * 3. stop the vm and detach the data volume
 * <p>
 * test the APILocalStorageGetVolumeMigratableHostsMsg
 */
public class TestLocalStorage37 {
    CLogger logger = Utils.getLogger(TestLocalStorage37.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;
    PrimaryStorageOverProvisioningManager psRatioMgr;
    long totalSize = SizeUnit.GIGABYTE.toByte(100);

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/localStorage/TestLocalStorage32.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("localStorageSimulator.xml");
        deployer.addSpringConfig("localStorage.xml");
        deployer.load();

        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(LocalStorageSimulatorConfig.class);
        psRatioMgr = loader.getComponent(PrimaryStorageOverProvisioningManager.class);

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
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        PrimaryStorageInventory local = deployer.primaryStorages.get("local");
        PrimaryStorageInventory local2 = deployer.primaryStorages.get("local2");
        api.stopVmInstance(vm.getUuid());

        VolumeInventory data = CollectionUtils.find(vm.getAllVolumes(),
                new Function<VolumeInventory, VolumeInventory>() {
                    @Override
                    public VolumeInventory call(VolumeInventory arg) {
                        return VolumeType.Data.toString().equals(arg.getType()) ? arg : null;
                    }
                });

        api.detachVolumeFromVm(data.getUuid());

        HostInventory host1 = deployer.hosts.get("host1");
        HostInventory host2 = deployer.hosts.get("host2");

        int spNum = 30;
        for (int i = 0; i < spNum; i++) {
            api.createSnapshot(data.getUuid());
        }

        LocalStorageHostRefVO hcap2 = new LocalStorageHostRefVOFinder().findByPrimaryKey(host2.getUuid(), local.getUuid());

        List<HostInventory> targets = api.getLocalStorageVolumeMigratableHost(data.getUuid(), null);
        Assert.assertEquals(1, targets.size());
        Assert.assertEquals(host2.getUuid(), targets.get(0).getUuid());

        // change the physical available capacity to 0
        long p = hcap2.getAvailablePhysicalCapacity();
        hcap2.setAvailablePhysicalCapacity(0);
        dbf.update(hcap2);
        targets = api.getLocalStorageVolumeMigratableHost(data.getUuid(), null);
        Assert.assertEquals(0, targets.size());

        hcap2.setAvailablePhysicalCapacity(p);
        dbf.update(hcap2);

        // set snapshot size to very big
        List<VolumeSnapshotVO> sps = dbf.listAll(VolumeSnapshotVO.class);
        for (VolumeSnapshotVO sp : sps) {
            sp.setSize(SizeUnit.GIGABYTE.toByte(1000));
        }
        dbf.updateCollection(sps);

        StopWatch watch = Utils.getStopWatch();
        watch.start();
        targets = api.getLocalStorageVolumeMigratableHost(data.getUuid(), null);
        watch.stop();
        logger.debug("xxxxxxxxxxx time: " + watch.getLapse());
        Assert.assertEquals(0, targets.size());
    }
}
