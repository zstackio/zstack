package org.zstack.test.kvm;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.simulator.storage.backup.sftp.SftpBackupStorageSimulatorConfig;
import org.zstack.simulator.storage.primary.nfs.NfsPrimaryStorageSimulatorConfig;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class TestCreateVmOnKvmSpecifiedPrimaryStorageForRootVolume {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDeleteImage2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    SftpBackupStorageSimulatorConfig config;
    NfsPrimaryStorageSimulatorConfig nconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestCreateVmOnKvmSpeicfiedPrimaryStorageForRootVolume.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(SftpBackupStorageSimulatorConfig.class);
        nconfig = loader.getComponent(NfsPrimaryStorageSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        PrimaryStorageInventory nfsPS = deployer.primaryStorages.get("nfs");

        L3NetworkInventory l3nInv1 = deployer.l3Networks.get("TestL3Network1");
        L3NetworkInventory l3nInv2 = deployer.l3Networks.get("TestL3Network2");
        L3NetworkInventory l3nInv3 = deployer.l3Networks.get("TestL3Network3");
        L3NetworkInventory l3nInv4 = deployer.l3Networks.get("TestL3Network4");

        InstanceOfferingInventory ioinv = deployer.instanceOfferings.get("TestInstanceOffering");
        ImageInventory img = deployer.images.get("TestImage");

        nconfig.downloadFromSftpCmds.clear();
        VmCreator creator = new VmCreator(api);
        creator.addL3Network(l3nInv1.getUuid());
        creator.addL3Network(l3nInv2.getUuid());
        creator.addL3Network(l3nInv3.getUuid());
        creator.addL3Network(l3nInv4.getUuid());
        creator.name = "vm";
        creator.imageUuid = img.getUuid();
        creator.instanceOfferingUuid = ioinv.getUuid();
        creator.primaryStorageUuidForRootVolume = nfsPS.getUuid();
        creator.create();
    }

}
