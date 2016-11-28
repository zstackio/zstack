package org.zstack.test.storage.primary.smp;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.primary.smp.SMPPrimaryStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * 1. use smp storage
 * 2. create a vm
 * 3. migrate the vm
 * <p>
 * confirm the vm migrated successfully
 * <p>
 * 4. clone a vm
 * <p>
 * confirm the vm cloned successfully
 * <p>
 * 5. attach a data volume
 * 6. create an image form the data volume
 * <p>
 * confirm the image created successfully
 * <p>
 * 7. take a snapshot from the data volume
 * 8. create a new data volume from the snapshot
 * <p>
 * confirm the data volume created successfully
 * <p>
 * 9. delete the snapshot
 * <p>
 * confirm the snapshot deleted successfully
 */
public class TestSmpPrimaryStorage2 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    SMPPrimaryStorageSimulatorConfig config;
    KVMSimulatorConfig kconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/smpPrimaryStorage/TestSmpPrimaryStorage.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("smpPrimaryStorageSimulator.xml");
        deployer.addSpringConfig("sharedMountPointPrimaryStorage.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        config = loader.getComponent(SMPPrimaryStorageSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        List<HostInventory> hosts = api.getMigrationTargetHost(vm.getUuid());
        HostInventory host = hosts.get(0);

        vm = api.migrateVmInstance(vm.getUuid(), host.getUuid());
        Assert.assertEquals(host.getUuid(), vm.getHostUuid());

        api.createVmFromClone(vm);

        DiskOfferingInventory diskOffering = deployer.diskOfferings.get("TestDiskOffering1");
        VolumeInventory data = api.createDataVolume("data", diskOffering.getUuid());
        api.attachVolumeToVm(vm.getUuid(), data.getUuid());
        Assert.assertEquals(1, config.createEmptyVolumeCmds.size());

        BackupStorageInventory sftp = deployer.backupStorages.get("sftp");
        data = api.detachVolumeFromVm(data.getUuid());
        ImageInventory image = api.addDataVolumeTemplateFromDataVolume(data.getUuid(), list(sftp.getUuid()));
        Assert.assertEquals(1, config.uploadBitsCmds.size());

        VolumeSnapshotInventory sp = api.createSnapshot(data.getUuid());
        api.createDataVolumeFromSnapshot(sp.getUuid());

        api.deleteSnapshot(sp.getUuid());
        Assert.assertEquals(1, config.offlineMergeSnapshotCmds.size());
    }
}
