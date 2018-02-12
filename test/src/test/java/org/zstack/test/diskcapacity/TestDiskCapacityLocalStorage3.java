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
import org.zstack.header.host.HostInventory;
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
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeType;
import org.zstack.simulator.storage.backup.sftp.SftpBackupStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageHostRefVO;
import org.zstack.storage.primary.local.LocalStorageHostRefVOFinder;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

import static org.zstack.utils.CollectionDSL.list;

/**
 * 1. use local storage
 * 2. add an image
 * 3. create a vm from the image
 * 4. create an image from the root volume
 * 5. create data volume and attach to the vm
 * <p>
 * confirm the size of image/volume are correct
 * confirm the local storage capacity correct
 */
public class TestDiskCapacityLocalStorage3 {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDeleteImage2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    SftpBackupStorageSimulatorConfig sconfig;
    LocalStorageSimulatorConfig lconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/diskcapacity/TestDiskCapacityLocalStorage1.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("localStorageSimulator.xml");
        deployer.addSpringConfig("localStorage.xml");
        deployer.load();

        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        sconfig = loader.getComponent(SftpBackupStorageSimulatorConfig.class);
        lconfig = loader.getComponent(LocalStorageSimulatorConfig.class);

        Capacity c = new Capacity();
        c.total = SizeUnit.TERABYTE.toByte(10);
        c.avail = c.total;

        lconfig.capacityMap.put("host1", c);

        deployer.build();
        api.prepare();
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
            msg.setSession(session);
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
                        lconfig.getVolumeSizeCmdActualSize.put(vol.getUuid(), rootVolumeActualSize);
                    }
                }
            }, InstantiateVolumeOnPrimaryStorageMsg.class);

            creator.session = session;
            creator.name = name;
            creator.imageUuid = imageUuid;
            creator.addL3Network(l3.getUuid());
            creator.instanceOfferingUuid = ioinv.getUuid();
            return creator.create();
        }
    }

    class AddDataVolume {
        String vmUuid;
        String diskOfferingUuid;

        VolumeInventory add() throws ApiSenderException {
            String uuid = Platform.getUuid();

            VolumeInventory vol = api.createDataVolume("data", diskOfferingUuid);
            vol = api.attachVolumeToVm(vmUuid, vol.getUuid());
            return vol;
        }
    }

    @Test
    public void test() throws ApiSenderException {
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

        // check root volume size
        Assert.assertEquals(addImage.size, root.getSize());
        Assert.assertEquals(addImage.actualSize, root.getActualSize().longValue());

        api.stopVmInstance(vm.getUuid());

        // check backup storage capacity after creating a template
        BackupStorageInventory sftp = deployer.backupStorages.get("sftp");
        BackupStorageVO bsbfore = dbf.findByUuid(sftp.getUuid(), BackupStorageVO.class);

        lconfig.getVolumeSizeCmdActualSize.put(root.getUuid(), root.getActualSize());
        lconfig.getVolumeSizeCmdSize.put(root.getUuid(), root.getSize());
        ImageInventory tmpt = api.createTemplateFromRootVolume("template", root.getUuid(), sftp.getUuid());
        Assert.assertEquals(root.getActualSize(), tmpt.getActualSize());
        Assert.assertEquals(root.getSize(), tmpt.getSize());

        BackupStorageVO bsAfter = dbf.findByUuid(sftp.getUuid(), BackupStorageVO.class);
        Assert.assertEquals(tmpt.getActualSize().longValue(), bsbfore.getAvailableCapacity() - bsAfter.getAvailableCapacity());

        // check local storage capacity
        PrimaryStorageInventory local = deployer.primaryStorages.get("local");
        PrimaryStorageCapacityVO pscap = dbf.findByUuid(local.getUuid(), PrimaryStorageCapacityVO.class);

        HostInventory host = deployer.hosts.get("host1");
        LocalStorageHostRefVO href = new LocalStorageHostRefVOFinder().findByPrimaryKey(host.getUuid(), local.getUuid());

        // image cache + volume
        long used = addImage.actualSize + root.getSize();
        long avail = pscap.getTotalCapacity() - used;
        Assert.assertEquals(avail, pscap.getAvailableCapacity());
        Assert.assertEquals(avail, href.getAvailableCapacity());

        // create a data volume, check local storage capacity
        DiskOfferingInventory doinv = deployer.diskOfferings.get("DataOffering");
        AddDataVolume addDataVolume = new AddDataVolume();
        addDataVolume.diskOfferingUuid = doinv.getUuid();
        addDataVolume.vmUuid = vm.getUuid();
        VolumeInventory data = addDataVolume.add();

        Assert.assertEquals(doinv.getDiskSize(), data.getSize());
        Assert.assertEquals(0, data.getActualSize().longValue());

        avail = avail - data.getSize();

        pscap = dbf.findByUuid(local.getUuid(), PrimaryStorageCapacityVO.class);
        Assert.assertEquals(avail, pscap.getAvailableCapacity());
        href = new LocalStorageHostRefVOFinder().findByPrimaryKey(host.getUuid(), local.getUuid());
        Assert.assertEquals(avail, href.getAvailableCapacity());

        // make the data volume some size
        long dataVolumeActualSize = SizeUnit.GIGABYTE.toByte(3);
        lconfig.getVolumeSizeCmdActualSize.put(data.getUuid(), dataVolumeActualSize);
        lconfig.getVolumeSizeCmdSize.put(data.getUuid(), data.getSize());
        ImageInventory dataTemplate = api.addDataVolumeTemplateFromDataVolume(data.getUuid(), list(sftp.getUuid()));
        Assert.assertEquals(data.getSize(), dataTemplate.getSize());
        Assert.assertEquals(dataVolumeActualSize, dataTemplate.getActualSize().longValue());

        BackupStorageVO bsAfter2 = dbf.findByUuid(sftp.getUuid(), BackupStorageVO.class);
        Assert.assertEquals(dataVolumeActualSize, bsAfter.getAvailableCapacity() - bsAfter2.getAvailableCapacity());
    }
}
