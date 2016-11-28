package org.zstack.test.diskcapacity;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.AsyncThread;
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
import org.zstack.header.storage.primary.InstantiateVolumeOnPrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;
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

import java.util.Random;
import java.util.concurrent.CountDownLatch;

import static org.zstack.utils.CollectionDSL.list;

/**
 * 1. use local storage
 * 2. add an image
 * 3. create 100 vm from the image
 * <p>
 * confirm the size of image/volume are correct
 * confirm the local storage capacity correct
 */

public class TestDiskCapacityLocalStorage2 {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDeleteImage2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    SftpBackupStorageSimulatorConfig sconfig;
    LocalStorageSimulatorConfig lconfig;
    int num = 100;
    CountDownLatch latch = new CountDownLatch(num);

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/diskcapacity/TestDiskCapacityLocalStorage1.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("localStorageSimulator.xml");
        deployer.addSpringConfig("localStorage.xml");
        deployer.load();

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

        api = deployer.getApi();
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
        boolean destroyed;

        @AsyncThread
        VmInstanceInventory create() throws ApiSenderException {
            try {
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
                VmInstanceInventory vm = creator.create();

                if (destroyed) {
                    api.destroyVmInstance(vm.getUuid());
                }
                return vm;
            } finally {
                latch.countDown();
            }
        }
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicy.Direct.toString());

        AddImage addImage = new AddImage();
        addImage.size = SizeUnit.GIGABYTE.toByte(10);
        addImage.actualSize = SizeUnit.GIGABYTE.toByte(1);
        ImageInventory image = addImage.add();

        Assert.assertEquals(image.getSize(), addImage.size);
        Assert.assertEquals(image.getActualSize().longValue(), addImage.actualSize);


        int destroyed = 0;
        for (int i = 0; i < num; i++) {
            CreateVm createVm = new CreateVm();
            createVm.imageUuid = image.getUuid();
            createVm.rootVolumeActualSize = addImage.actualSize;
            createVm.destroyed = new Random().nextBoolean();
            if (createVm.destroyed) {
                destroyed++;
            }
            VmInstanceInventory vm = createVm.create();
        }

        latch.await();

        PrimaryStorageInventory local = deployer.primaryStorages.get("local");
        PrimaryStorageCapacityVO pscap = dbf.findByUuid(local.getUuid(), PrimaryStorageCapacityVO.class);

        HostInventory host = deployer.hosts.get("host1");
        LocalStorageHostRefVO href = new LocalStorageHostRefVOFinder().findByPrimaryKey(host.getUuid(), local.getUuid());

        // image cache + volume
        long used = addImage.actualSize + (num - destroyed) * addImage.size;
        long avail = pscap.getTotalCapacity() - used;
        Assert.assertEquals(avail, pscap.getAvailableCapacity());
        Assert.assertEquals(avail, href.getAvailableCapacity());
    }
}
