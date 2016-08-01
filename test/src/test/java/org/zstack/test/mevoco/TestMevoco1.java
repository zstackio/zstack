package org.zstack.test.mevoco;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.host.APIAddHostEvent;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.message.AbstractBeforeDeliveryMessageInterceptor;
import org.zstack.header.message.Message;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.primary.DownloadImageToPrimaryStorageCacheMsg;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.primary.ImageCacheVO_;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.kvm.APIAddKVMHostMsg;
import org.zstack.mevoco.MevocoGlobalConfig;
import org.zstack.network.service.flat.FlatNetworkServiceSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageKvmSftpBackupStorageMediatorImpl.SftpDownloadBitsCmd;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.data.SizeUnit;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 1. add an image
 *
 * confirm the image distributed to the host1
 *
 * 2. add a new host
 *
 * confirm the image distributed to the host2
 *
 * 3. delete the image cache
 * 4. reconnect the host2
 *
 * confirm the image distributed to the host2
 *
 * 5. reconnect host1
 *
 * confirm no images get re-downloaded
 */
public class TestMevoco1 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;
    FlatNetworkServiceSimulatorConfig fconfig;
    long totalSize = SizeUnit.GIGABYTE.toByte(100);
    boolean success = true;

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

    ImageCacheVO findImageOnHost(String prUuid, String imgUuid, String hostUuid) {
        SimpleQuery<ImageCacheVO> q = dbf.createQuery(ImageCacheVO.class);
        q.add(ImageCacheVO_.imageUuid, Op.EQ, imgUuid);
        q.add(ImageCacheVO_.primaryStorageUuid, Op.EQ, prUuid);
        List<ImageCacheVO> caches = q.list();
        for (ImageCacheVO c : caches) {
            if (c.getInstallUrl().contains(hostUuid)) {
                return c;
            }
        }

        return null;
    }

	@Test
	public void test() throws ApiSenderException, InterruptedException {
	    MevocoGlobalConfig.DISTRIBUTE_IMAGE.updateValue(true);
        BackupStorageInventory sftp = deployer.backupStorages.get("sftp");
        PrimaryStorageInventory local = deployer.primaryStorages.get("local");
        ImageInventory img = new ImageInventory();
        img.setName("image");
        img.setPlatform(ImagePlatform.Linux.toString());
        img.setMediaType(ImageMediaType.RootVolumeTemplate.toString());
        img.setFormat("qcow2");
        img.setUrl("http://test.img");

        config.downloadBitsCmds.clear();
        img = api.addImage(img, sftp.getUuid());

        TimeUnit.SECONDS.sleep(3);

        Assert.assertFalse(config.downloadBitsCmds.isEmpty());
        SftpDownloadBitsCmd cmd = config.downloadBitsCmds.get(0);
        Assert.assertEquals(img.getBackupStorageRefs().get(0).getInstallPath(), cmd.getBackupStorageInstallPath());

        HostInventory host1 = deployer.hosts.get("host1");
        Assert.assertNotNull(findImageOnHost(local.getUuid(), img.getUuid(), host1.getUuid()));

        config.downloadBitsCmds.clear();
        APIAddKVMHostMsg msg = new APIAddKVMHostMsg();
        msg.setSession(api.getAdminSession());
        msg.setName("host2");
        msg.setClusterUuid(host1.getClusterUuid());
        msg.setManagementIp("127.0.0.1");
        msg.setUsername("root");
        msg.setPassword("password");
        ApiSender sender = api.getApiSender();
        APIAddHostEvent evt = sender.send(msg, APIAddHostEvent.class);
        HostInventory host2 = evt.getInventory();
        TimeUnit.SECONDS.sleep(3);

        Assert.assertFalse(config.downloadBitsCmds.isEmpty());
        ImageCacheVO cacheVO = findImageOnHost(local.getUuid(), img.getUuid(), host2.getUuid());
        Assert.assertNotNull(cacheVO);

        dbf.remove(cacheVO);
        config.downloadBitsCmds.clear();
        api.reconnectHost(host2.getUuid());
        TimeUnit.SECONDS.sleep(3);
        Assert.assertFalse(config.downloadBitsCmds.isEmpty());
        cacheVO = findImageOnHost(local.getUuid(), img.getUuid(), host2.getUuid());
        Assert.assertNotNull(cacheVO);

        bus.installBeforeDeliveryMessageInterceptor(new AbstractBeforeDeliveryMessageInterceptor() {
            @Override
            public void intercept(Message msg) {
                success = false;
            }
        }, DownloadImageToPrimaryStorageCacheMsg.class);
        api.reconnectHost(host1.getUuid());
        TimeUnit.SECONDS.sleep(3);
        Assert.assertTrue(success);
    }
}
