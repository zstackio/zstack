package org.zstack.test.storage.ceph;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * 1. has two ceph primary storage
 * 2. create data volumes on them with specified primary storage uuid
 * <p>
 * confirm volumes created on right primary storage
 */
public class TestCephCreateDataVolume {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDeleteImage2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/ceph/TestCephCreateDataVolume.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("ceph.xml");
        deployer.addSpringConfig("cephSimulator.xml");
        deployer.load();
        loader = deployer.getComponentLoader();

        deployer.build();
        api = deployer.getApi();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        PrimaryStorageInventory ceph1 = deployer.primaryStorages.get("ceph-pri1");
        PrimaryStorageInventory ceph2 = deployer.primaryStorages.get("ceph-pri2");

        DiskOfferingInventory diskOffering = deployer.diskOfferings.get("DataOffering");

        PrimaryStorageCapacityVO nfs1Cap1 = dbf.findByUuid(ceph1.getUuid(), PrimaryStorageCapacityVO.class);
        VolumeInventory vol = api.createDataVolume("data1", diskOffering.getUuid(), ceph1.getUuid(), null);
        Assert.assertEquals(VolumeStatus.Ready.toString(), vol.getStatus());
        Assert.assertEquals(ceph1.getUuid(), vol.getPrimaryStorageUuid());
        PrimaryStorageCapacityVO nfs1Cap2 = dbf.findByUuid(ceph1.getUuid(), PrimaryStorageCapacityVO.class);
        Assert.assertEquals(vol.getSize(), nfs1Cap1.getAvailableCapacity() - nfs1Cap2.getAvailableCapacity());

        vol = api.createDataVolume("data2", diskOffering.getUuid(), ceph2.getUuid(), null);
        Assert.assertEquals(VolumeStatus.Ready.toString(), vol.getStatus());
        Assert.assertEquals(ceph2.getUuid(), vol.getPrimaryStorageUuid());
    }
}
