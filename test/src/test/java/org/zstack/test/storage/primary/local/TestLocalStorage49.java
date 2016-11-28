package org.zstack.test.storage.primary.local;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.primary.ImageCacheVO_;
import org.zstack.image.ImageGlobalProperty;
import org.zstack.image.ImageUpgradeExtension;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.data.SizeUnit;

/**
 * 1. test get missing uuid for image cache
 * <p>
 * confirm it works
 */
public class TestLocalStorage49 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;
    long totalSize = SizeUnit.GIGABYTE.toByte(100);
    ImageUpgradeExtension imgUpgradeExtension;

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
        imgUpgradeExtension = loader.getComponent(ImageUpgradeExtension.class);

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
        ImageInventory image = deployer.images.get("TestImage");
        SimpleQuery<ImageCacheVO> q = dbf.createQuery(ImageCacheVO.class);
        q.add(ImageCacheVO_.imageUuid, Op.EQ, image.getUuid());
        ImageCacheVO c = q.find();
        c.setImageUuid(null);
        c.setInstallUrl(String.format("file:///zstack_ps/imagecache/template/2bc455e8fe844f7b9c82b00a38777a0e/%s.qcow2;hostUuid://da7de0d650654e6f949d6e9521d84eea", image.getUuid()));
        dbf.update(c);

        ImageGlobalProperty.FIX_IMAGE_CACHE_UUID = true;
        imgUpgradeExtension.start();
        c = dbf.findById(c.getId(), ImageCacheVO.class);
        junit.framework.Assert.assertEquals(image.getUuid(), c.getImageUuid());
    }
}
