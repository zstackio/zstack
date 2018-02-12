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
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.list;

/**
 * 1. use nfs storage
 * 2. create a vm
 * 3. attach a data volume to the vm
 * 4. create snapshots from the root volume and the data volume
 * 5. delay to delete the vm and data volume
 * <p>
 * confirm the capacity of the nfs storage correct
 * <p>
 * 6. expunge the vm
 * 7. expunge the data volume
 * <p>
 * confirm the capacity of the nfs storage correct
 */
public class TestDiskCapacityNfs5 {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDeleteImage2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    SftpBackupStorageSimulatorConfig sconfig;
    NfsPrimaryStorageSimulatorConfig nconfig;
    KVMSimulatorConfig kconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/diskcapacity/TestDiskCapacityNfs1.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        sconfig = loader.getComponent(SftpBackupStorageSimulatorConfig.class);
        nconfig = loader.getComponent(NfsPrimaryStorageSimulatorConfig.class);
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

        DiskOfferingInventory doinv = deployer.diskOfferings.get("DataOffering");
        VolumeInventory data = api.createDataVolume("data", doinv.getUuid());
        data = api.attachVolumeToVm(vm.getUuid(), data.getUuid());

        long rootSpSize = SizeUnit.GIGABYTE.toByte(2);
        kconfig.takeSnapshotCmdSize.put(root.getUuid(), rootSpSize);
        long dataSpSize = SizeUnit.GIGABYTE.toByte(3);
        kconfig.takeSnapshotCmdSize.put(data.getUuid(), dataSpSize);
        VolumeSnapshotInventory spRoot = api.createSnapshot(root.getUuid());
        VolumeSnapshotInventory spData = api.createSnapshot(data.getUuid());

        PrimaryStorageInventory nfs = deployer.primaryStorages.get("nfs");
        PrimaryStorageCapacityVO pscap = dbf.findByUuid(nfs.getUuid(), PrimaryStorageCapacityVO.class);

        api.destroyVmInstance(vm.getUuid());

        // delay delete, primary storage capacity not changed
        PrimaryStorageCapacityVO pscap1 = dbf.findByUuid(nfs.getUuid(), PrimaryStorageCapacityVO.class);
        Assert.assertEquals(pscap.getAvailableCapacity(), pscap1.getAvailableCapacity());

        api.deleteDataVolume(data.getUuid());

        // delay delete, primary storage capacity not changed
        PrimaryStorageCapacityVO pscap2 = dbf.findByUuid(nfs.getUuid(), PrimaryStorageCapacityVO.class);
        Assert.assertEquals(pscap.getAvailableCapacity(), pscap2.getAvailableCapacity());

        api.expungeVm(vm.getUuid(), null);
        TimeUnit.SECONDS.sleep(2);

        PrimaryStorageCapacityVO pscap3 = dbf.findByUuid(nfs.getUuid(), PrimaryStorageCapacityVO.class);
        // image cache + data volume + data volume snapshot
        long used = addImage.actualSize + data.getSize() + spData.getSize();
        long avail = pscap3.getTotalCapacity() - used;
        Assert.assertEquals(avail, pscap3.getAvailableCapacity());

        api.expungeDataVolume(data.getUuid(), null);
        TimeUnit.SECONDS.sleep(2);

        PrimaryStorageCapacityVO pscap4 = dbf.findByUuid(nfs.getUuid(), PrimaryStorageCapacityVO.class);
        // image cache
        used = addImage.actualSize;
        avail = pscap4.getTotalCapacity() - used;
        Assert.assertEquals(avail, pscap4.getAvailableCapacity());
    }
}
