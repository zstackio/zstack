package org.zstack.test.core.scheduler;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.scheduler.SchedulerFacade;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.header.volume.VolumeType;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.volume.TestCreateDataVolume;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

/**
 * Created by root on 8/24/16.
 */
public class TestSchedulerChangeVolumeStatus {
    CLogger logger = Utils.getLogger(TestCreateDataVolume.class);
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;
    CloudBus bus;
    Deployer deployer;
    @Autowired
    SchedulerFacade scheduler;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        BeanConstructor con = new WebBeanConstructor();
        /* This loads spring application context */
        deployer = new Deployer("deployerXml/kvm/TestCreateVmOnKvm.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("SchedulerFacade.xml");
        loader = con.addXml("PortalForUnitTest.xml").addXml("Simulator.xml").addXml("ZoneManager.xml")
                .addXml("PrimaryStorageManager.xml").addXml("ConfigurationManager.xml").addXml("VolumeManager.xml")
                .addXml("AccountManager.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        bus = loader.getComponent(CloudBus.class);
        scheduler = loader.getComponent(SchedulerFacade.class);
        api = new Api();
        api.startServer();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        api = deployer.getApi();
        Assert.assertNotNull(scheduler);
        DiskOfferingInventory dinv = new DiskOfferingInventory();
        dinv.setDiskSize(SizeUnit.GIGABYTE.toByte(10));
        dinv.setName("Test");
        dinv.setDescription("Test");
        dinv = api.addDiskOffering(dinv);
        VolumeInventory vinv = api.createDataVolume("TestData", dinv.getUuid());
        Assert.assertEquals(VolumeStatus.NotInstantiated.toString(), vinv.getStatus());
        Assert.assertEquals(VolumeType.Data.toString(), vinv.getType());
        Assert.assertFalse(vinv.isAttached());
        Integer interval = 3;
        String type="simple";
        Long startDate=0L;
        api.createVolumeSnapshotScheduler(vinv.getUuid(), null, type, startDate, interval, null);
        //destroy volume
        TimeUnit.SECONDS.sleep(2);
        long record = dbf.count(VolumeSnapshotVO.class);
        Assert.assertEquals(1,record);
        api.deleteDataVolume(vinv.getUuid());
        TimeUnit.SECONDS.sleep(3);
        long record2 = dbf.count(VolumeSnapshotVO.class);
        Assert.assertEquals(1,record2);

        //recover volume
        api.recoverVolume(vinv.getUuid(), null);
        TimeUnit.SECONDS.sleep(4);
        long record3 = dbf.count(VolumeSnapshotVO.class);
        Assert.assertEquals(2,record3);

        //expunge volume
        api.expungeDataVolume(vinv.getUuid(), null);
        TimeUnit.SECONDS.sleep(4);
        long record4 = dbf.count(VolumeSnapshotVO.class);
        Assert.assertEquals(2,record4);
    }

}
