package org.zstack.test.applianceVm;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.identity.IdentityCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by miao on 16-9-2.
 */
public class TestChangeOwnerOfVm {
    CLogger logger = Utils.getLogger(TestChangeOwnerOfVm.class);
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
        deployer = new Deployer("deployerXml/vm/TestChangeOwnerOfVm.xml", con);
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
        IdentityCreator identityCreator = new IdentityCreator(api);
        identityCreator.useAccount("test");
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        DiskOfferingInventory dinv = deployer.diskOfferings.get("TestDataDiskOffering1");
        VolumeInventory vi = api.createDataVolume("data", dinv.getUuid());
        api.attachVolumeToVm(vm.getUuid(), vi.getUuid());
        // create volume snapshot
        String volUuid = vm.getRootVolumeUuid();
        VolumeSnapshotInventory inv = api.createSnapshot(volUuid);

        // change owner for the same account
        try {
            api.changeResourceOwner(vm.getUuid(), identityCreator.getAccountSession().getAccountUuid());
        } catch (Exception e) {

        }

        // change owner for the different account
        identityCreator.useAccount("test2");
        String targetAccountUuid = identityCreator.getAccountSession().getAccountUuid();
        api.changeResourceOwner(vm.getUuid(), targetAccountUuid);
        ArrayList<String> resUuids = new ArrayList<>();
        resUuids.add(vm.getRootVolumeUuid());
        resUuids.add(vi.getUuid());
        // add vmnics of vm
        SimpleQuery<VmNicVO> sq = dbf.createQuery(VmNicVO.class);
        sq.select(VmNicVO_.uuid);
        sq.add(VmNicVO_.vmInstanceUuid, SimpleQuery.Op.EQ, vm.getUuid());
        List<String> vmnics = sq.listValue();
        if (vmnics.isEmpty()) {
            return;
        }
        for (String vmnicUuid : vmnics) {
            logger.debug("VmNic:" + vmnicUuid);
            resUuids.add(vmnicUuid);
        }
        //
        Map<String, AccountInventory> resAccMap = api.getResourceAccount(resUuids);
        assert (resAccMap.size() == resUuids.size());

        for (AccountInventory ai : resAccMap.values()) {
            logger.debug("begin");
            logger.debug(ai.getUuid());
            logger.debug(ai.getType());
            logger.debug("targetAccountUuid:" + targetAccountUuid);
            logger.debug("end");
            assert (ai.getUuid().equals(targetAccountUuid));
        }
    }
}

