package org.zstack.test.storage.primary.local;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.volume.APICreateDataVolumeEvent;
import org.zstack.header.volume.APICreateDataVolumeMsg;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.storage.primary.local.LocalStorageResourceRefVO;
import org.zstack.storage.primary.local.LocalStorageResourceRefVO_;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.storage.primary.local.LocalStorageSystemTags;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * 1. has two local primary storage
 * 2. create data volumes on them with specified primary storage uuid
 * <p>
 * confirm volumes created on right primary storage
 */
public class TestLocalStorageCreateDataVolume {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDeleteImage2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/localStorage/TestLocalStorageCreateDataVolume.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("localStorageSimulator.xml");
        deployer.addSpringConfig("localStorage.xml");
        deployer.load();
        loader = deployer.getComponentLoader();
        config = loader.getComponent(LocalStorageSimulatorConfig.class);

        long totalSize = SizeUnit.TERABYTE.toByte(2);
        Capacity c = new Capacity();
        c.total = totalSize;
        c.avail = totalSize;

        config.capacityMap.put("host1", c);
        config.capacityMap.put("host2", c);

        deployer.build();
        api = deployer.getApi();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        session = api.loginAsAdmin();
    }

    private VolumeInventory createDataVolume(String psUuid, String hostUuid) throws ApiSenderException {
        DiskOfferingInventory diskOffering = deployer.diskOfferings.get("DataOffering");
        ApiSender sender = new ApiSender();
        APICreateDataVolumeMsg msg = new APICreateDataVolumeMsg();
        msg.setSession(api.getAdminSession());
        msg.setPrimaryStorageUuid(psUuid);
        msg.addSystemTag(LocalStorageSystemTags.DEST_HOST_FOR_CREATING_DATA_VOLUME.instantiateTag(
                map(e(LocalStorageSystemTags.DEST_HOST_FOR_CREATING_DATA_VOLUME_TOKEN, hostUuid))
        ));
        msg.setName("data");
        msg.setDiskOfferingUuid(diskOffering.getUuid());
        APICreateDataVolumeEvent e = sender.send(msg, APICreateDataVolumeEvent.class);
        return e.getInventory();
    }

    @Test
    public void test() throws ApiSenderException {
        PrimaryStorageInventory local = deployer.primaryStorages.get("local");
        HostInventory host1 = deployer.hosts.get("host1");
        HostInventory host2 = deployer.hosts.get("host2");

        PrimaryStorageCapacityVO nfs1Cap1 = dbf.findByUuid(local.getUuid(), PrimaryStorageCapacityVO.class);
        VolumeInventory vol = createDataVolume(local.getUuid(), host1.getUuid());
        Assert.assertEquals(VolumeStatus.Ready.toString(), vol.getStatus());
        Assert.assertEquals(local.getUuid(), vol.getPrimaryStorageUuid());
        PrimaryStorageCapacityVO nfs1Cap2 = dbf.findByUuid(local.getUuid(), PrimaryStorageCapacityVO.class);
        Assert.assertEquals(vol.getSize(), nfs1Cap1.getAvailableCapacity() - nfs1Cap2.getAvailableCapacity());
        LocalStorageResourceRefVO ref1 = Q.New(LocalStorageResourceRefVO.class)
                .eq(LocalStorageResourceRefVO_.resourceUuid, vol.getUuid())
                .find();
        Assert.assertEquals(host1.getUuid(), ref1.getHostUuid());

        vol = createDataVolume(local.getUuid(), host2.getUuid());
        Assert.assertEquals(VolumeStatus.Ready.toString(), vol.getStatus());
        LocalStorageResourceRefVO ref2 = Q.New(LocalStorageResourceRefVO.class)
                .eq(LocalStorageResourceRefVO_.resourceUuid, vol.getUuid())
                .find();
        Assert.assertEquals(host2.getUuid(), ref2.getHostUuid());
    }
}
