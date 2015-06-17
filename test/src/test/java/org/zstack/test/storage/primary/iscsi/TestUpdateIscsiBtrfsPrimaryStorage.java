package org.zstack.test.storage.primary.iscsi;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.KVMAgentCommands.StartVmCmd;
import org.zstack.kvm.KVMAgentCommands.VolumeTO;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.primary.iscsi.IscsiFileSystemBackendPrimaryStorageInventory;
import org.zstack.storage.primary.iscsi.IscsiFileSystemBackendPrimaryStorageVO;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

/**
 * 1. create VM with IscsiBtrfsPrimaryStorage
 *
 * confirm success
 */
public class TestUpdateIscsiBtrfsPrimaryStorage {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    KVMSimulatorConfig kconfig;

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
    }
    
    @Test
    public void test() throws ApiSenderException, InterruptedException {
        IscsiFileSystemBackendPrimaryStorageInventory inv = (IscsiFileSystemBackendPrimaryStorageInventory) deployer.primaryStorages.get("TestPrimaryStorage");
        inv.setName("1");
        inv.setDescription("xxx");
        inv.setChapUsername("2");
        inv.setChapPassword("3");
        inv.setSshUsername("4");
        inv.setSshPassword("5");
        inv = api.updateIscsiFileSystemPrimaryStorage(inv);

        Assert.assertEquals("1", inv.getName());
        Assert.assertEquals("xxx", inv.getDescription());
        Assert.assertEquals("2", inv.getChapUsername());
        Assert.assertEquals("3", inv.getChapPassword());
        Assert.assertEquals("4", inv.getSshUsername());
        Assert.assertEquals("5", inv.getSshPassword());
    }
}
