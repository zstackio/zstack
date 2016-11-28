package org.zstack.test.storage.primary.local;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.query.QueryOp;
import org.zstack.header.storage.snapshot.APIQueryVolumeSnapshotMsg;
import org.zstack.header.storage.snapshot.APIQueryVolumeSnapshotReply;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.APIQueryVolumeMsg;
import org.zstack.header.volume.APIQueryVolumeReply;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.storage.primary.local.APIQueryLocalStorageResourceRefMsg;
import org.zstack.storage.primary.local.APIQueryLocalStorageResourceRefReply;
import org.zstack.storage.primary.local.LocalStorageResourceRefInventory;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.function.Function;

import java.util.List;

/**
 * 1. use local storage
 * 2. create a vm
 * <p>
 * confirm all local storage related commands, VOs are set
 */
public class TestQueryLocalStorage {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;
    long totalSize = SizeUnit.GIGABYTE.toByte(100);

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/localStorage/TestLocalStorage1.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("localStorageSimulator.xml");
        deployer.addSpringConfig("localStorage.xml");
        deployer.load();

        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(LocalStorageSimulatorConfig.class);

        Capacity c = new Capacity();
        c.total = totalSize;
        c.avail = totalSize;

        config.capacityMap.put("host1", c);

        deployer.build();
        api = deployer.getApi();
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        HostInventory host = deployer.hosts.get("host1");
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VolumeInventory root = vm.getRootVolume();

        APIQueryVolumeMsg vmsg = new APIQueryVolumeMsg();
        vmsg.addQueryCondition("localStorageHostRef.hostUuid", QueryOp.EQ, host.getUuid());
        APIQueryVolumeReply vr = api.query(vmsg, APIQueryVolumeReply.class);
        Assert.assertEquals(1, vr.getInventories().size());
        VolumeInventory vol = vr.getInventories().get(0);
        Assert.assertEquals(root.getUuid(), vol.getUuid());

        VolumeSnapshotInventory sp = api.createSnapshot(root.getUuid());
        APIQueryVolumeSnapshotMsg smsg = new APIQueryVolumeSnapshotMsg();
        smsg.addQueryCondition("localStorageHostRef.hostUuid", QueryOp.EQ, host.getUuid());
        APIQueryVolumeSnapshotReply sr = api.query(smsg, APIQueryVolumeSnapshotReply.class);
        Assert.assertEquals(1, sr.getInventories().size());
        VolumeSnapshotInventory sp1 = sr.getInventories().get(0);
        Assert.assertEquals(sp.getUuid(), sp1.getUuid());

        APIQueryLocalStorageResourceRefMsg lmsg = new APIQueryLocalStorageResourceRefMsg();
        lmsg.addQueryCondition("hostUuid", QueryOp.EQ, host.getUuid());
        APIQueryLocalStorageResourceRefReply lr = api.query(lmsg, APIQueryLocalStorageResourceRefReply.class);
        List<String> resourceUuid = CollectionUtils.transformToList(lr.getInventories(), new Function<String, LocalStorageResourceRefInventory>() {
            @Override
            public String call(LocalStorageResourceRefInventory arg) {
                return arg.getResourceUuid();
            }
        });

        Assert.assertTrue(resourceUuid.contains(root.getUuid()));
        Assert.assertTrue(resourceUuid.contains(sp.getUuid()));
    }
}
