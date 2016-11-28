package org.zstack.test.storage.snapshot;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.snapshot.APIQueryVolumeSnapshotTreeMsg;
import org.zstack.header.storage.snapshot.APIQueryVolumeSnapshotTreeReply;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotTreeInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.search.QueryTestValidator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/*
* take snapshot from vm's root volume
* and query snapshot tree
*/
public class TestQuerySnapshotTree {
    CLogger logger = Utils.getLogger(TestQuerySnapshotTree.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;

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
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        String volUuid = vm.getRootVolumeUuid();
        VolumeSnapshotInventory inv = api.createSnapshot(volUuid);
        VolumeSnapshotTreeInventory tree = api.getVolumeSnapshotTree(inv.getTreeUuid(), null).get(0);

        QueryTestValidator.validateEQ(new APIQueryVolumeSnapshotTreeMsg(), api, APIQueryVolumeSnapshotTreeReply.class, tree);
        QueryTestValidator.validateRandomEQConjunction(new APIQueryVolumeSnapshotTreeMsg(), api, APIQueryVolumeSnapshotTreeReply.class, tree, 3);
    }

}
