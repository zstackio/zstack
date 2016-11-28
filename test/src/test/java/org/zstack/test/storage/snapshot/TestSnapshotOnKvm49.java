package org.zstack.test.storage.snapshot;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeType;
import org.zstack.simulator.kvm.VolumeSnapshotKvmSimulator;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.identity.IdentityCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/*
* test volumesnapshot ownership changed after VM/data volume ownership changed
*/
public class TestSnapshotOnKvm49 {
    CLogger logger = Utils.getLogger(TestSnapshotOnKvm49.class);
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
        deployer = new Deployer("deployerXml/volumeSnapshot/TestVolumeSnapshot49.xml", con);
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
        AccountInventory test = deployer.accounts.get("test");
        VmInstanceInventory vm = deployer.vms.get("TestVm");

        String volUuid = vm.getRootVolumeUuid();
        SessionInventory testSession = api.loginByAccount("test", "password");
        VolumeSnapshotInventory inv = api.createSnapshot(volUuid, testSession);

        VolumeInventory data = vm.getAllVolumes().stream().filter(v -> VolumeType.Data.toString().equals(v.getType())).findAny().get();
        VolumeSnapshotInventory dataSP = api.createSnapshot(data.getUuid(), testSession);

        IdentityCreator creator = new IdentityCreator(api);
        creator.createAccount("user", "password");
        api.changeResourceOwner(vm.getUuid(), creator.getAccountSession().getAccountUuid());
        api.deleteSnapshot(inv.getUuid(), creator.getAccountSession());

        boolean hasException = false;
        try {
            api.changeResourceOwner(data.getUuid(), creator.getAccountSession().getAccountUuid());
        } catch (ApiSenderException ex) {
            hasException = true;
            logger.warn("changeResourceOwner failed: ", ex);
        }

        Assert.assertTrue("expected change resource owner exception", hasException);

        api.deleteSnapshot(dataSP.getUuid(), creator.getAccountSession());
    }
}
