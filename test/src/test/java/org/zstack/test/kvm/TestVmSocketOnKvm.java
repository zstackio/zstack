package org.zstack.test.kvm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * 1 create a vm from a data volume template
 * <p>
 * confirm the vm failed to create
 */
public class TestVmSocketOnKvm {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDeleteImage2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    KVMSimulatorConfig config;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestVmSocketOnKvm.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(KVMSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        // windows
        ImageInventory img1 = deployer.images.get("TestImage1");
        // windows virtio
        ImageInventory img2 = deployer.images.get("TestImage2");
        // linux
        ImageInventory img3 = deployer.images.get("TestImage3");

        InstanceOfferingInventory ioinv1 = deployer.instanceOfferings.get("TestInstanceOffering1");
        InstanceOfferingInventory ioinv2 = deployer.instanceOfferings.get("TestInstanceOffering2");
        InstanceOfferingInventory ioinv3 = deployer.instanceOfferings.get("TestInstanceOffering3");

        L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network1");

        // socket = 1 cpu = 1
        VmCreator creator = new VmCreator(api);
        creator.addL3Network(l3.getUuid());
        creator.name = "vm";
        creator.imageUuid = img1.getUuid();
        creator.instanceOfferingUuid = ioinv1.getUuid();
        creator.create();

        KVMAgentCommands.StartVmCmd cmd = config.startVmCmd;
        Assert.assertEquals(1, cmd.getSocketNum());
        Assert.assertEquals(1, cmd.getCpuOnSocket());

        // socket = 2 cpu = 1
        creator = new VmCreator(api);
        creator.addL3Network(l3.getUuid());
        creator.name = "vm1";
        creator.imageUuid = img1.getUuid();
        creator.instanceOfferingUuid = ioinv2.getUuid();
        creator.create();

        cmd = config.startVmCmd;
        Assert.assertEquals(2, cmd.getSocketNum());
        Assert.assertEquals(1, cmd.getCpuOnSocket());

        // socket = 3 cpu = 1
        creator = new VmCreator(api);
        creator.addL3Network(l3.getUuid());
        creator.name = "vm1";
        creator.imageUuid = img2.getUuid();
        creator.instanceOfferingUuid = ioinv3.getUuid();
        creator.create();

        cmd = config.startVmCmd;
        Assert.assertEquals(3, cmd.getSocketNum());
        Assert.assertEquals(1, cmd.getCpuOnSocket());

        // socket = 1 cpu = 3
        creator = new VmCreator(api);
        creator.addL3Network(l3.getUuid());
        creator.name = "vm1";
        creator.imageUuid = img3.getUuid();
        creator.instanceOfferingUuid = ioinv3.getUuid();
        creator.create();

        cmd = config.startVmCmd;
        Assert.assertEquals(1, cmd.getSocketNum());
        Assert.assertEquals(3, cmd.getCpuOnSocket());
    }

}
