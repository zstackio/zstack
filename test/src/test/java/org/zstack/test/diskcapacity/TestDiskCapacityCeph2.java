package org.zstack.test.diskcapacity;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.APIAddImageEvent;
import org.zstack.header.image.APIAddImageMsg;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.AbstractBeforeDeliveryMessageInterceptor;
import org.zstack.header.message.Message;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.primary.InstantiateVolumeOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeType;
import org.zstack.storage.ceph.backup.CephBackupStorageSimulatorConfig;
import org.zstack.storage.ceph.primary.CephPrimaryStorageSimulatorConfig;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

public class TestDiskCapacityCeph2 {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDeleteImage2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    CephPrimaryStorageSimulatorConfig cconfig;
    CephBackupStorageSimulatorConfig bconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/diskcapacity/TestDiskCapacityCeph1.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("ceph.xml");
        deployer.addSpringConfig("cephSimulator.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        cconfig = loader.getComponent(CephPrimaryStorageSimulatorConfig.class);
        bconfig = loader.getComponent(CephBackupStorageSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    class AddImage {
        long size;
        long actualSize;
        String name = "image";
        BackupStorageInventory sftp = deployer.backupStorages.get("ceph-bk");

        ImageInventory add() throws ApiSenderException {
            String uuid = Platform.getUuid();

            bconfig.imageSize.put(uuid, size);
            bconfig.imageActualSize.put(uuid, actualSize);

            APIAddImageMsg msg = new APIAddImageMsg();
            msg.setResourceUuid(uuid);
            msg.setName(name);
            msg.setBackupStorageUuids(list(sftp.getUuid()));
            msg.setFormat("qcow2");
            msg.setUrl("http://image.qcow2");
            msg.setSession(api.getAdminSession());
            ApiSender sender = new ApiSender();
            APIAddImageEvent evt = sender.send(msg, APIAddImageEvent.class);
            return evt.getInventory();
        }
    }

    class CreateVm {
        VmCreator creator = new VmCreator(api);
        String name = "vm";
        String imageUuid;
        long rootVolumeActualSize;

        VmInstanceInventory create() throws ApiSenderException {
            InstanceOfferingInventory ioinv = deployer.instanceOfferings.get("TestInstanceOffering");
            L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network1");

            bus.installBeforeDeliveryMessageInterceptor(new AbstractBeforeDeliveryMessageInterceptor() {
                @Override
                public void intercept(Message msg) {
                    InstantiateVolumeOnPrimaryStorageMsg imsg = (InstantiateVolumeOnPrimaryStorageMsg) msg;
                    VolumeInventory vol = imsg.getVolume();
                    if (VolumeType.Root.toString().equals(vol.getType())) {
                        cconfig.getVolumeActualSizeCmdSize.put(vol.getUuid(), rootVolumeActualSize);
                    }
                }
            }, InstantiateVolumeOnPrimaryStorageMsg.class);

            creator.name = name;
            creator.imageUuid = imageUuid;
            creator.addL3Network(l3.getUuid());
            creator.instanceOfferingUuid = ioinv.getUuid();
            return creator.create();
        }
    }

    private long getSnapshotSize(String volumeUuid) {
        SimpleQuery<VolumeSnapshotVO> q = dbf.createQuery(VolumeSnapshotVO.class);
        q.add(VolumeSnapshotVO_.volumeUuid, Op.EQ, volumeUuid);
        List<VolumeSnapshotVO> sps = q.list();
        long s = 0;
        for (VolumeSnapshotVO sp : sps) {
            s += sp.getSize();
        }
        return s;
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        AddImage addImage = new AddImage();
        addImage.size = SizeUnit.GIGABYTE.toByte(10);
        addImage.actualSize = SizeUnit.GIGABYTE.toByte(1);
        ImageInventory image = addImage.add();

        Assert.assertEquals(image.getSize(), addImage.size);
        Assert.assertEquals(image.getActualSize().longValue(), addImage.actualSize);

        CreateVm createVm = new CreateVm();
        createVm.imageUuid = image.getUuid();
        createVm.rootVolumeActualSize = addImage.actualSize;
        VmInstanceInventory vm = createVm.create();
        VolumeInventory root = vm.getRootVolume();

        Assert.assertEquals(addImage.size, root.getSize());
        Assert.assertEquals(addImage.actualSize, root.getActualSize().longValue());

        PrimaryStorageInventory ceph = deployer.primaryStorages.get("ceph-pri");
        PrimaryStorageCapacityVO pscap = dbf.findByUuid(ceph.getUuid(), PrimaryStorageCapacityVO.class);
        // image cache + volumes + snapshots
        long used = addImage.actualSize + root.getSize();
        long avail = pscap.getTotalCapacity() - used;
        Assert.assertEquals(avail, pscap.getAvailableCapacity());

        DiskOfferingInventory doinv = deployer.diskOfferings.get("DataOffering");
        VolumeInventory data = api.createDataVolume("data", doinv.getUuid());
        data = api.attachVolumeToVm(vm.getUuid(), data.getUuid());
        Assert.assertEquals(doinv.getDiskSize(), data.getSize());

        pscap = dbf.findByUuid(ceph.getUuid(), PrimaryStorageCapacityVO.class);
        // image cache + root volume + data volume
        used = addImage.actualSize + root.getSize() + data.getSize();
        avail = pscap.getTotalCapacity() - used;
        Assert.assertEquals(avail, pscap.getAvailableCapacity());

        long rootSpSize = SizeUnit.GIGABYTE.toByte(2);
        cconfig.createSnapshotCmdSize.put(root.getUuid(), rootSpSize);
        VolumeSnapshotInventory rootSp = api.createSnapshot(root.getUuid());
        Assert.assertEquals(rootSpSize, rootSp.getSize());

        long dataSpSize = SizeUnit.GIGABYTE.toByte(3);
        cconfig.createSnapshotCmdSize.put(data.getUuid(), dataSpSize);
        VolumeSnapshotInventory dataSp = api.createSnapshot(data.getUuid());
        Assert.assertEquals(dataSpSize, dataSp.getSize());

        pscap = dbf.findByUuid(ceph.getUuid(), PrimaryStorageCapacityVO.class);

        BackupStorageInventory cephBs = deployer.backupStorages.get("ceph-bk");
        BackupStorageVO bs1 = dbf.findByUuid(cephBs.getUuid(), BackupStorageVO.class);
        long templateSize = SizeUnit.GIGABYTE.toByte(2);
        cconfig.getVolumeSizeCmdSize.put(rootSp.getVolumeUuid(), templateSize);
        long templateActualSize = SizeUnit.GIGABYTE.toByte(1);
        cconfig.getVolumeSizeCmdActualSize.put(rootSp.getVolumeUuid(), templateActualSize);
        ImageInventory template = api.createTemplateFromSnapshot(rootSp.getUuid(), cephBs.getUuid());
        Assert.assertEquals(templateSize, template.getSize());
        Assert.assertEquals(templateActualSize + getSnapshotSize(rootSp.getVolumeUuid()), template.getActualSize().longValue());

        BackupStorageVO bs2 = dbf.findByUuid(cephBs.getUuid(), BackupStorageVO.class);
        Assert.assertEquals(template.getActualSize().longValue(), bs1.getAvailableCapacity() - bs2.getAvailableCapacity());

        // primary storage capacity not changed
        PrimaryStorageCapacityVO pscap1 = dbf.findByUuid(ceph.getUuid(), PrimaryStorageCapacityVO.class);
        Assert.assertEquals(pscap.getAvailableCapacity(), pscap1.getAvailableCapacity());

        templateSize = SizeUnit.GIGABYTE.toByte(2);
        cconfig.cpCmdSize.put(dataSp.getVolumeUuid(), templateSize);
        templateActualSize = SizeUnit.GIGABYTE.toByte(1);
        cconfig.cpCmdActualSize.put(dataSp.getVolumeUuid(), templateActualSize);
        VolumeInventory data1 = api.createDataVolumeFromSnapshot(dataSp.getUuid());
        Assert.assertEquals(templateSize, data1.getSize());
        Assert.assertEquals(templateActualSize, data1.getActualSize().longValue());

        long volumeSize = SizeUnit.GIGABYTE.toByte(2);
        cconfig.getVolumeSizeCmdSize.put(data.getUuid(), volumeSize);
        long volumeActualSize = SizeUnit.GIGABYTE.toByte(1);
        cconfig.getVolumeSizeCmdActualSize.put(data.getUuid(), volumeActualSize);
        data = api.syncVolumeSize(data.getUuid(), null);
        Assert.assertEquals(volumeSize, data.getSize());
        Assert.assertEquals(volumeActualSize + getSnapshotSize(data.getUuid()), data.getActualSize().longValue());
    }
}
