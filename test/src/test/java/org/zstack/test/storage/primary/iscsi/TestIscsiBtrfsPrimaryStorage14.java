package org.zstack.test.storage.primary.iscsi;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.primary.iscsi.IscsiBtrfsPrimaryStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

/**
 * 1. create VM with IscsiBtrfsPrimaryStorage
 * 2. take two snapshots from VM's root volume
 * 3. revert the volume to the snapshot 1
 *
 * confirm the volume is successfully reverted
 */
public class TestIscsiBtrfsPrimaryStorage14 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    KVMSimulatorConfig kconfig;
    IscsiBtrfsPrimaryStorageSimulatorConfig iconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/iscsiBtrfsPrimaryStorage/TestIscsiBtrfsPrimaryStorage.xml", con);
        deployer.addSpringConfig("iscsiBtrfsPrimaryStorage.xml");
        deployer.addSpringConfig("iscsiFileSystemPrimaryStorageSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        iconfig = loader.getComponent(IscsiBtrfsPrimaryStorageSimulatorConfig.class);
    }
    
    @Test
    public void test() throws ApiSenderException, InterruptedException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VolumeInventory root = CollectionUtils.find(vm.getAllVolumes(), new Function<VolumeInventory, VolumeInventory>() {
            @Override
            public VolumeInventory call(VolumeInventory arg) {
                return VolumeType.Root.toString().equals(arg.getType()) ? arg : null;
            }
        });

        assert root != null;
        VolumeSnapshotInventory sp1 =  api.createSnapshot(root.getUuid());
        IscsiBtrfsSnapshotValidator validator = new IscsiBtrfsSnapshotValidator();
        validator.validate(sp1);
        Assert.assertEquals(1, iconfig.createSubVolumeCmds.size());

        VolumeSnapshotInventory sp2 =  api.createSnapshot(root.getUuid());
        validator.validate(sp2);
        Assert.assertEquals(2, iconfig.createSubVolumeCmds.size());

        api.revertVolumeToSnapshot(sp1.getUuid());
        VolumeVO rootvo = dbf.findByUuid(root.getUuid(), VolumeVO.class);
        Assert.assertEquals(sp1.getPrimaryStorageInstallPath(), rootvo.getInstallPath());
    }
}
