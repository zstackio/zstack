package org.zstack.test.diskcapacity;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmGlobalConfig;
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
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeDeletionPolicyManager.VolumeDeletionPolicy;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeType;
import org.zstack.simulator.storage.backup.sftp.SftpBackupStorageSimulatorConfig;
import org.zstack.simulator.storage.primary.nfs.NfsPrimaryStorageSimulatorConfig;
import org.zstack.storage.volume.VolumeGlobalConfig;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

import static org.zstack.utils.CollectionDSL.list;

public class TestDiskCapacityNfs3 {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDeleteImage2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    SftpBackupStorageSimulatorConfig sconfig;
    NfsPrimaryStorageSimulatorConfig nconfig;

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
    public void test() throws ApiSenderException {
        AddImage addImage = new AddImage();
        addImage.size = SizeUnit.GIGABYTE.toByte(1);
        addImage.actualSize = SizeUnit.GIGABYTE.toByte(10);
        ImageInventory image = addImage.add();

        CreateVm createVm = new CreateVm();
        createVm.imageUuid = image.getUuid();
        createVm.rootVolumeActualSize = addImage.actualSize;
        VmInstanceInventory vm = createVm.create();
        VolumeInventory root = vm.getRootVolume();

        api.stopVmInstance(vm.getUuid());

        PrimaryStorageInventory nfs = deployer.primaryStorages.get("nfs");
        PrimaryStorageCapacityVO pscap = dbf.findByUuid(nfs.getUuid(), PrimaryStorageCapacityVO.class);

        nconfig.getVolumeSizeCmdActualSize.put(root.getUuid(), root.getActualSize());
        nconfig.getVolumeSizeCmdSize.put(root.getUuid(), root.getSize());
        BackupStorageInventory sftp = deployer.backupStorages.get("sftp");
        ImageInventory template = api.createTemplateFromRootVolume("root", root.getUuid(), list(sftp.getUuid()));
        Assert.assertEquals(root.getSize(), template.getSize());
        Assert.assertEquals(root.getActualSize(), template.getActualSize());

        // primary storage capacity not changed
        PrimaryStorageCapacityVO pscap1 = dbf.findByUuid(nfs.getUuid(), PrimaryStorageCapacityVO.class);
        Assert.assertEquals(pscap1.getAvailableCapacity(), pscap.getAvailableCapacity());

        DiskOfferingInventory doinv = deployer.diskOfferings.get("DataOffering");
        VolumeInventory data = api.createDataVolume("data", doinv.getUuid());
        data = api.attachVolumeToVm(vm.getUuid(), data.getUuid());
        Assert.assertEquals(doinv.getDiskSize(), data.getSize());

        // image cache + root volume + data volume
        long used = addImage.actualSize + root.getSize() + data.getSize();
        PrimaryStorageCapacityVO pscap2 = dbf.findByUuid(nfs.getUuid(), PrimaryStorageCapacityVO.class);
        long avail = pscap2.getTotalCapacity() - used;
        Assert.assertEquals(avail, pscap2.getAvailableCapacity());

        VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicy.Direct.toString());
        api.destroyVmInstance(vm.getUuid());
        VolumeGlobalConfig.VOLUME_DELETION_POLICY.updateValue(VolumeDeletionPolicy.Direct.toString());
        api.deleteDataVolume(data.getUuid());

        // only image cache
        used = addImage.actualSize;
        PrimaryStorageCapacityVO pscap3 = dbf.findByUuid(nfs.getUuid(), PrimaryStorageCapacityVO.class);
        avail = pscap3.getTotalCapacity() - used;
        Assert.assertEquals(avail, pscap3.getAvailableCapacity());
    }
}
