package org.zstack.test.storage.primary.local;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageDeletionPolicyManager.ImageDeletionPolicy;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.primary.ImageCacheVO_;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.image.ImageGlobalConfig;
import org.zstack.storage.primary.PrimaryStorageGlobalConfig;
import org.zstack.storage.primary.local.LocalStorageKvmBackend.CacheInstallPath;
import org.zstack.storage.primary.local.LocalStorageKvmBackend.DeleteBitsCmd;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.data.SizeUnit;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * 1. one host, one vm with a data volume
 * 2. delete the image the vm created from
 * 3. delete the vm
 * <p>
 * confirm the image is garbage collected
 */
public class TestLocalStorage55 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;
    long totalSize = SizeUnit.GIGABYTE.toByte(100);

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/localStorage/TestLocalStorage50.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("localStorageSimulator.xml");
        deployer.addSpringConfig("localStorage.xml");
        deployer.load();

        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(LocalStorageSimulatorConfig.class);

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
        VmInstanceInventory vm1 = deployer.vms.get("TestVm");
        DiskOfferingInventory doinv = deployer.diskOfferings.get("TestDiskOffering1");

        VolumeInventory data = api.createDataVolume("data", doinv.getUuid());
        api.attachVolumeToVm(vm1.getUuid(), data.getUuid());

        ImageGlobalConfig.DELETION_POLICY.updateValue(ImageDeletionPolicy.Direct.toString());
        ImageInventory image1 = deployer.images.get("TestImage");
        api.deleteImage(image1.getUuid());

        SimpleQuery<ImageCacheVO> q = dbf.createQuery(ImageCacheVO.class);
        q.add(ImageCacheVO_.imageUuid, Op.EQ, image1.getUuid());
        ImageCacheVO c = q.find();

        VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicy.Direct.toString());
        api.destroyVmInstance(vm1.getUuid());

        CacheInstallPath path = new CacheInstallPath();
        path.fullPath = c.getInstallUrl();
        path.disassemble();

        config.deleteBitsCmds.clear();
        PrimaryStorageGlobalConfig.IMAGE_CACHE_GARBAGE_COLLECTOR_INTERVAL.updateValue(1);
        TimeUnit.SECONDS.sleep(3);

        Assert.assertEquals(1, config.deleteDirCmds.size());
        DeleteBitsCmd cmd = config.deleteDirCmds.get(0);
        Assert.assertEquals(new File(path.installPath).getParent(), cmd.getPath());
        c = q.find();
        Assert.assertNull(c);

        ImageInventory image2 = deployer.images.get("TestImage2");
        q = dbf.createQuery(ImageCacheVO.class);
        q.add(ImageCacheVO_.imageUuid, Op.EQ, image2.getUuid());
        c = q.find();
        Assert.assertNotNull(c);
    }
}
