package org.zstack.test.diskcapacity;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
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
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeType;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.storage.backup.sftp.SftpBackupStorageSimulatorConfig;
import org.zstack.simulator.storage.primary.nfs.NfsPrimaryStorageSimulatorConfig;
import org.zstack.storage.primary.smp.SMPPrimaryStorageSimulatorConfig;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

import static org.zstack.utils.CollectionDSL.list;

public class TestDiskCapacitySmp2 {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDeleteImage2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    SftpBackupStorageSimulatorConfig sconfig;
    NfsPrimaryStorageSimulatorConfig nconfig;
    SMPPrimaryStorageSimulatorConfig smpconfig;
    KVMSimulatorConfig kconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/diskcapacity/TestDiskCapacitySmp1.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("smpPrimaryStorageSimulator.xml");
        deployer.addSpringConfig("sharedMountPointPrimaryStorage.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        sconfig = loader.getComponent(SftpBackupStorageSimulatorConfig.class);
        nconfig = loader.getComponent(NfsPrimaryStorageSimulatorConfig.class);
        smpconfig = loader.getComponent(SMPPrimaryStorageSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    class AddImage {
        long size;
        long actualSize;
        String name = "image";
        BackupStorageInventory sftp = deployer.backupStorages.get("sftp");

        ImageInventory add() throws ApiSenderException {
            String uuid = Platform.getUuid();

            sconfig.imageSizes.put(uuid, size);
            sconfig.imageActualSizes.put(uuid, actualSize);

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
                        nconfig.getVolumeSizeCmdActualSize.put(vol.getUuid(), rootVolumeActualSize);
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

        DiskOfferingInventory doinv = deployer.diskOfferings.get("DataOffering");
        VolumeInventory data = api.createDataVolume("data", doinv.getUuid());
        data = api.attachVolumeToVm(vm.getUuid(), data.getUuid());
        Assert.assertEquals(doinv.getDiskSize(), data.getSize());

        long rootSpSize = SizeUnit.GIGABYTE.toByte(2);
        kconfig.takeSnapshotCmdSize.put(root.getUuid(), rootSpSize);
        VolumeSnapshotInventory rootSp = api.createSnapshot(root.getUuid());
        Assert.assertEquals(rootSpSize, rootSp.getSize());

        long dataSpSize = SizeUnit.GIGABYTE.toByte(3);
        kconfig.takeSnapshotCmdSize.put(data.getUuid(), dataSpSize);
        VolumeSnapshotInventory dataSp = api.createSnapshot(data.getUuid());
        Assert.assertEquals(dataSpSize, dataSp.getSize());

        PrimaryStorageInventory nfs = deployer.primaryStorages.get("smp");
        PrimaryStorageCapacityVO pscap = dbf.findByUuid(nfs.getUuid(), PrimaryStorageCapacityVO.class);

        // image cache + volumes + snapshots
        long used = addImage.actualSize + root.getSize() + data.getSize() + rootSpSize + dataSpSize;
        long avail = pscap.getTotalCapacity() - used;
        Assert.assertEquals(avail, pscap.getAvailableCapacity());

        // create an image from the root volume snapshot
        BackupStorageInventory sftp = deployer.backupStorages.get("sftp");
        BackupStorageVO bs = dbf.findByUuid(sftp.getUuid(), BackupStorageVO.class);

        long templateSize = SizeUnit.GIGABYTE.toByte(2);
        smpconfig.mergeSnapshotCmdSize.put(rootSp.getVolumeUuid(), templateSize);
        long templateActualSize = SizeUnit.GIGABYTE.toByte(1);
        smpconfig.mergeSnapshotCmdActualSize.put(rootSp.getVolumeUuid(), templateActualSize);
        ImageInventory template = api.createTemplateFromSnapshot(rootSp.getUuid(), sftp.getUuid());
        Assert.assertEquals(templateSize, template.getSize());
        Assert.assertEquals(templateActualSize, template.getActualSize().longValue());

        BackupStorageVO bs1 = dbf.findByUuid(sftp.getUuid(), BackupStorageVO.class);
        Assert.assertEquals(bs.getAvailableCapacity() - bs1.getAvailableCapacity(), templateActualSize);

        // create a data volume from a snapshot
        long data1Size = SizeUnit.GIGABYTE.toByte(3);
        smpconfig.mergeSnapshotCmdSize.put(dataSp.getVolumeUuid(), data1Size);
        long data1ActualSize = SizeUnit.GIGABYTE.toByte(1);
        smpconfig.mergeSnapshotCmdActualSize.put(dataSp.getVolumeUuid(), data1ActualSize);
        VolumeInventory data1 = api.createDataVolumeFromSnapshot(dataSp.getUuid());
        Assert.assertEquals(data1Size, data1.getSize());
        Assert.assertEquals(data1ActualSize, data1.getActualSize().longValue());

        PrimaryStorageCapacityVO pscap1 = dbf.findByUuid(nfs.getUuid(), PrimaryStorageCapacityVO.class);
        Assert.assertEquals(data1Size, pscap.getAvailableCapacity() - pscap1.getAvailableCapacity());
    }
}
