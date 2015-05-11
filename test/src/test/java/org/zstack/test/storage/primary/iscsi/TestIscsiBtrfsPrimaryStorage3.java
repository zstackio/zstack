package org.zstack.test.storage.primary.iscsi;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.KVMAgentCommands.StartVmCmd;
import org.zstack.kvm.KVMAgentCommands.VolumeTO;
import org.zstack.kvm.KVMConstant;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.primary.iscsi.IscsiBtrfsPrimaryStorageSimulatorConfig;
import org.zstack.storage.primary.iscsi.IscsiFileSystemBackendPrimaryStorageVO;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

import java.util.List;

/**
 * 1. create VM with IscsiBtrfsPrimaryStorage
 * 2. stop vm
 * 3. create template from vm's root volume
 *
 * confirm success and root volume is of format raw
 */
public class TestIscsiBtrfsPrimaryStorage3 {
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
        api.stopVmInstance(vm.getUuid());
        ImageInventory i = api.createTemplateFromRootVolume("t1", vm.getRootVolumeUuid(), (List)null);
        Assert.assertEquals(KVMConstant.RAW_FORMAT_STRING, i.getFormat());
        Assert.assertEquals(1, iconfig.uploadToSftpCmds.size());
    }
}
