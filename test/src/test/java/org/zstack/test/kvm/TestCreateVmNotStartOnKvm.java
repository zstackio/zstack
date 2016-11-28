package org.zstack.test.kvm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.jsonlabel.JsonLabel;
import org.zstack.core.jsonlabel.JsonLabelVO;
import org.zstack.header.allocator.HostCapacityVO;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmCreationStrategy;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.volume.VolumeVO;
import org.zstack.simulator.storage.backup.sftp.SftpBackupStorageSimulatorConfig;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class TestCreateVmNotStartOnKvm {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDeleteImage2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    SftpBackupStorageSimulatorConfig config;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestCreateVmNotStartOnKvm.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(SftpBackupStorageSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws InterruptedException, ApiSenderException {
        InstanceOfferingInventory inv = deployer.instanceOfferings.get("TestInstanceOffering");
        ImageInventory image = deployer.images.get("TestImage");
        L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network1");
        DiskOfferingInventory data = deployer.diskOfferings.get("TestRootDiskOffering");

        VmCreator creator = new VmCreator(api);
        creator.strategy = VmCreationStrategy.JustCreate;
        creator.addDisk(data.getUuid());
        creator.imageUuid = image.getUuid();
        creator.addL3Network(l3.getUuid());
        creator.instanceOfferingUuid = inv.getUuid();
        creator.name = "vm";
        VmInstanceInventory vm = creator.create();

        Assert.assertEquals(VmInstanceState.Created.toString(), vm.getState());

        HostInventory host = deployer.hosts.get("host1");
        HostCapacityVO cap = dbf.findByUuid(host.getUuid(), HostCapacityVO.class);
        Assert.assertEquals(cap.getTotalCpu(), cap.getAvailableCpu());
        Assert.assertEquals(cap.getTotalMemory(), cap.getAvailableMemory());
        Assert.assertEquals(0, dbf.count(VolumeVO.class));

        vm = api.startVmInstance(vm.getUuid());
        cap = dbf.findByUuid(host.getUuid(), HostCapacityVO.class);
        Assert.assertEquals(cap.getTotalCpu() - vm.getCpuNum(), cap.getAvailableCpu());
        Assert.assertEquals(cap.getTotalMemory() - vm.getMemorySize(), cap.getAvailableMemory());
        Assert.assertEquals(2, dbf.count(VolumeVO.class));

        new JsonLabel().create("1", "2");
        new JsonLabel().delete("1");
        VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicy.Direct.toString());
        api.destroyVmInstance(vm.getUuid());
        Assert.assertEquals(0, dbf.count(JsonLabelVO.class));
    }

}
