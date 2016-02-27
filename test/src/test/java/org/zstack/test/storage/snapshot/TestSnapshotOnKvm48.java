package org.zstack.test.storage.snapshot;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.AbstractBeforeDeliveryMessageInterceptor;
import org.zstack.header.message.Message;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.kvm.VolumeSnapshotKvmSimulator;
import org.zstack.simulator.storage.primary.nfs.NfsPrimaryStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/*
* take snapshot from vm's root volume
*/
public class TestSnapshotOnKvm48 {
    CLogger logger = Utils.getLogger(TestSnapshotOnKvm48.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    VolumeSnapshotKvmSimulator snapshotKvmSimulator;
    boolean success = true;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestCreateVmOnKvm.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        snapshotKvmSimulator = loader.getComponent(VolumeSnapshotKvmSimulator.class);
        session = api.loginAsAdmin();
    }
    
	@Test
	public void test() throws ApiSenderException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        L2NetworkInventory l2 = deployer.l2Networks.get("TestL2Network");
        String volUuid = vm.getRootVolumeUuid();
        VolumeSnapshotInventory inv = api.createSnapshot(volUuid);

        bus.installBeforeDeliveryMessageInterceptor(new AbstractBeforeDeliveryMessageInterceptor() {
            @Override
            public void intercept(Message msg) {
                success = false;
            }
        }, VolumeSnapshotPrimaryStorageDeletionMsg.class);

        api.deleteL2Network(l2.getUuid());
        VolumeSnapshotVO sp = dbf.findByUuid(inv.getUuid(), VolumeSnapshotVO.class);
        Assert.assertNotNull(sp);
        Assert.assertTrue(success);
    }
}
