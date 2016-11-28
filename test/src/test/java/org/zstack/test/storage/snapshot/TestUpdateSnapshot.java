package org.zstack.test.storage.snapshot;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.simulator.kvm.VolumeSnapshotKvmSimulator;
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
public class TestUpdateSnapshot {
    CLogger logger = Utils.getLogger(TestUpdateSnapshot.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    VolumeSnapshotKvmSimulator snapshotKvmSimulator;

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
        String volUuid = vm.getRootVolumeUuid();
        VolumeSnapshotInventory inv = api.createSnapshot(volUuid);
        inv.setName("1");
        inv.setDescription("xxx");
        inv = api.updateVolumeSnapshot(inv);
        Assert.assertEquals("1", inv.getName());
        Assert.assertEquals("xxx", inv.getDescription());
    }
}
