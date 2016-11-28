package org.zstack.test.storage.primary.local;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.storage.volume.VolumeGlobalProperty;
import org.zstack.storage.volume.VolumeUpgradeExtension;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.data.SizeUnit;

import java.util.concurrent.TimeUnit;

/**
 * 1. set volume root image uuid to NULL
 * 2. use VolumeUpgradeExtension to get the missing uuid back
 * <p>
 * confirm it works
 */
public class TestLocalStorage48 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;
    long totalSize = SizeUnit.GIGABYTE.toByte(100);
    VolumeUpgradeExtension volumeUpgradeExtension;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/localStorage/TestLocalStorage1.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("localStorageSimulator.xml");
        deployer.addSpringConfig("localStorage.xml");
        deployer.load();

        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(LocalStorageSimulatorConfig.class);
        volumeUpgradeExtension = loader.getComponent(VolumeUpgradeExtension.class);

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
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        String imageUuid = vm.getRootVolume().getRootImageUuid();
        config.getVolumeBaseImagePaths.put(vm.getRootVolumeUuid(), String.format("/%s.qcow2", imageUuid));
        VolumeVO vol = dbf.findByUuid(vm.getRootVolumeUuid(), VolumeVO.class);
        vol.setRootImageUuid(null);
        dbf.update(vol);

        VolumeGlobalProperty.ROOT_VOLUME_FIND_MISSING_IMAGE_UUID = true;
        volumeUpgradeExtension.start();

        PrimaryStorageInventory local = deployer.primaryStorages.get("local");
        PrimaryStorageInventory local2 = deployer.primaryStorages.get("local2");

        api.reconnectPrimaryStorage(local.getUuid());
        TimeUnit.SECONDS.sleep(3);
        api.reconnectPrimaryStorage(local2.getUuid());
        TimeUnit.SECONDS.sleep(3);

        vol = dbf.findByUuid(vm.getRootVolumeUuid(), VolumeVO.class);
        Assert.assertEquals(imageUuid, vol.getRootImageUuid());
    }
}
