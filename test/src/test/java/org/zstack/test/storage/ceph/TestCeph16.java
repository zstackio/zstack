package org.zstack.test.storage.ceph;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageDeletionPolicyManager.ImageDeletionPolicy;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.primary.ImageCacheVO_;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.image.ImageGlobalConfig;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.ceph.CephGlobalConfig;
import org.zstack.storage.ceph.primary.CephPrimaryStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

import java.util.concurrent.TimeUnit;

/**
 * 0. change the cleanup interval of image cache to 1s
 * 1. use ceph for backup storage and primary storage
 * 2. create two vm
 * 3. destroy one vm
 * <p>
 * confirm the image cache is not cleaned up
 */
public class TestCeph16 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    CephPrimaryStorageSimulatorConfig config;
    KVMSimulatorConfig kconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/ceph/TestCeph16.xml", con);
        deployer.addSpringConfig("ceph.xml");
        deployer.addSpringConfig("cephSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(CephPrimaryStorageSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicy.Direct.toString());
        CephGlobalConfig.IMAGE_CACHE_CLEANUP_INTERVAL.updateValue(1);
        ImageGlobalConfig.DELETION_POLICY.updateValue(ImageDeletionPolicy.Direct.toString());
        TimeUnit.SECONDS.sleep(5);

        ImageInventory img = deployer.images.get("TestImage");
        SimpleQuery<ImageCacheVO> q = dbf.createQuery(ImageCacheVO.class);
        q.add(ImageCacheVO_.imageUuid, Op.EQ, img.getUuid());
        ImageCacheVO c = q.find();
        Assert.assertNotNull(c);

        api.deleteImage(img.getUuid());
        TimeUnit.SECONDS.sleep(3);
        Assert.assertTrue(q.isExists());

        VmInstanceInventory vm = deployer.vms.get("TestVm");
        api.destroyVmInstance(vm.getUuid());

        config.deleteCmds.clear();
        TimeUnit.SECONDS.sleep(3);
        Assert.assertTrue(q.isExists());
        Assert.assertTrue(config.deleteImageCacheCmds.isEmpty());
    }
}
