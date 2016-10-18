package org.zstack.test.storage.primary.local;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
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
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.function.Function;

import java.util.concurrent.TimeUnit;

/**
 * 1. use local storage
 * 2. create a vm with data volume
 * 3. stop the vm and detach the data volume
 * 4. delete the image, set the backing file existing on the dst
 * 5. migrate the root volume to host2
 * 7. make the migrate cmd fail
 * <p>
 * confirm the migration failed but the backing file not deleted on the dst
 */
public class TestLocalStorage36 {
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
        ImageInventory image = deployer.images.get("TestImage");
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        PrimaryStorageInventory local = deployer.primaryStorages.get("local");
        api.stopVmInstance(vm.getUuid());

        VolumeInventory data = CollectionUtils.find(vm.getAllVolumes(), new Function<VolumeInventory, VolumeInventory>() {
            @Override
            public VolumeInventory call(VolumeInventory arg) {
                return VolumeType.Data.toString().equals(arg.getType()) ? arg : null;
            }
        });

        api.detachVolumeFromVm(data.getUuid());

        config.backingFilePath = image.getBackupStorageRefs().get(0).getInstallPath();
        config.backingFileSize = image.getSize();
        config.checkBitsSuccess = true;
        config.copyBitsFromRemoteSuccess = false;

        api.deleteImage(image.getUuid());

        VolumeInventory root = vm.getRootVolume();

        HostInventory host2 = deployer.hosts.get("host2");
        LocalStorageHostRefVO hcap2 = new LocalStorageHostRefVOFinder().findByPrimaryKey(host2.getUuid(), local.getUuid());

        boolean s = false;
        try {
            api.localStorageMigrateVolume(root.getUuid(), host2.getUuid(), null);
        } catch (ApiSenderException e) {
            s = true;
        }
        Assert.assertTrue(s);

        TimeUnit.SECONDS.sleep(3);
        Assert.assertEquals(0, config.deleteBitsCmds.size());

        LocalStorageHostRefVO hcap22 = new LocalStorageHostRefVOFinder().findByPrimaryKey(host2.getUuid(), local.getUuid());
        Assert.assertEquals(hcap2.getAvailableCapacity(), hcap22.getAvailableCapacity());
    }
}
