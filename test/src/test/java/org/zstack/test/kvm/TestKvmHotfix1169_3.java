package org.zstack.test.kvm;

import junit.framework.Assert;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.AbstractService;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.AbstractBeforeSendMessageInterceptor;
import org.zstack.header.message.Message;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotTree;
import org.zstack.header.storage.snapshot.VolumeSnapshotTree.SnapshotLeaf;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.hotfix.HotFix1169Result;
import org.zstack.kvm.KvmRunShellMsg;
import org.zstack.kvm.KvmRunShellReply;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.snapshot.VolumeSnapshotGlobalConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/*
* take snapshot from vm's root volume
*/
public class TestKvmHotfix1169_3 {
    CLogger logger = Utils.getLogger(TestKvmHotfix1169_3.class);
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
        deployer = new Deployer("deployerXml/kvm/TestCreateVmOnKvm.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("hotfix.xml");
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
        VolumeSnapshotGlobalConfig.MAX_INCREMENTAL_SNAPSHOT_NUM.updateValue(5);

        VmInstanceInventory vm = deployer.vms.get("TestVm");
        String volUuid = vm.getRootVolumeUuid();

        // create 6 snapshots to make a new tree
        api.createSnapshot(volUuid);
        api.createSnapshot(volUuid);
        api.createSnapshot(volUuid);
        api.createSnapshot(volUuid);
        api.createSnapshot(volUuid);
        api.createSnapshot(volUuid);

        VolumeSnapshotInventory inv1 = api.createSnapshot(volUuid);
        Assert.assertNull(inv1.getParentUuid());
        VolumeSnapshotInventory inv2 = api.createSnapshot(volUuid);
        VolumeVO root = dbf.findByUuid(volUuid, VolumeVO.class);

        List<String> treeOnStorage = new ArrayList<>();
        treeOnStorage.add(String.format("%s %s %s %s", inv1.getPrimaryStorageInstallPath(), "NONE",
                inv1.getSize(), 0));
        treeOnStorage.add(String.format("%s %s %s %s", inv2.getPrimaryStorageInstallPath(), inv1.getPrimaryStorageInstallPath(),
                inv2.getSize(), 0));
        // the missing one
        treeOnStorage.add(String.format("%s %s %s %s", root.getInstallPath(), inv2.getPrimaryStorageInstallPath(),
                root.getSize(), 0));
        // the new volume
        String newPath = PathUtil.join(new File(root.getInstallPath()).getParent(), "abcd.qcow2");
        treeOnStorage.add(String.format("%s %s %s %s", newPath, root.getInstallPath(),
                SizeUnit.BYTE.toByte(100), 0));

        bus.installBeforeSendMessageInterceptor(new AbstractBeforeSendMessageInterceptor() {
            @Override
            public void intercept(Message msg) {
                if (msg instanceof KvmRunShellMsg) {
                    bus.makeLocalServiceId(msg, TestKvmHotfix1169_3.class.getName());
                }
            }
        }, KvmRunShellMsg.class);

        bus.registerService(new AbstractService() {
            @Override
            public void handleMessage(Message msg) {
                if (msg instanceof KvmRunShellMsg) {
                    KvmRunShellReply reply = new KvmRunShellReply();
                    reply.setReturnCode(0);
                    reply.setStdout(StringUtils.join(treeOnStorage, "\n"));
                    bus.reply(msg, reply);
                } else {
                    bus.dealWithUnknownMessage(msg);
                }
            }

            @Override
            public String getId() {
                return bus.makeLocalServiceId(TestKvmHotfix1169_3.class.getName());
            }

            @Override
            public boolean start() {
                return true;
            }

            @Override
            public boolean stop() {
                return true;
            }
        });

        List<HotFix1169Result> results = api.hotfix1169(root.getPrimaryStorageUuid());
        Assert.assertEquals(1, results.size());

        HotFix1169Result res = results.get(0);
        Assert.assertTrue(res.success);
        Assert.assertEquals(root.getUuid(), res.volumeUuid);
        Assert.assertEquals(root.getName(), res.volumeName);

        SimpleQuery<VolumeSnapshotVO> q = dbf.createQuery(VolumeSnapshotVO.class);
        q.add(VolumeSnapshotVO_.treeUuid, Op.EQ, inv1.getTreeUuid());
        List<VolumeSnapshotVO> sps = q.list();

        Assert.assertEquals(3, sps.size());
        VolumeSnapshotTree tree = VolumeSnapshotTree.fromVOs(sps);
        SnapshotLeaf sp1 = tree.findSnapshot(new Function<Boolean, VolumeSnapshotInventory>() {
            @Override
            public Boolean call(VolumeSnapshotInventory arg) {
                return arg.getPrimaryStorageInstallPath().equals(inv1.getPrimaryStorageInstallPath());
            }
        });
        Assert.assertNotNull(sp1);
        Assert.assertEquals(inv1.getSize(), sp1.getInventory().getSize());
        Assert.assertNull(sp1.getParent());
        VolumeSnapshotVO sp = dbf.findByUuid(sp1.getUuid(), VolumeSnapshotVO.class);
        Assert.assertEquals(0, sp.getDistance());

        SnapshotLeaf sp2 = tree.findSnapshot(new Function<Boolean, VolumeSnapshotInventory>() {
            @Override
            public Boolean call(VolumeSnapshotInventory arg) {
                return arg.getPrimaryStorageInstallPath().equals(inv2.getPrimaryStorageInstallPath());
            }
        });
        Assert.assertNotNull(sp2);
        Assert.assertEquals(inv2.getSize(), sp2.getInventory().getSize());
        Assert.assertNotNull(sp2.getParent());
        Assert.assertEquals(inv1.getPrimaryStorageInstallPath(), sp2.getParent().getInventory().getPrimaryStorageInstallPath());
        sp = dbf.findByUuid(sp2.getUuid(), VolumeSnapshotVO.class);
        Assert.assertEquals(1, sp.getDistance());

        SnapshotLeaf sp3 = tree.findSnapshot(new Function<Boolean, VolumeSnapshotInventory>() {
            @Override
            public Boolean call(VolumeSnapshotInventory arg) {
                return arg.getPrimaryStorageInstallPath().equals(root.getInstallPath());
            }
        });
        Assert.assertNotNull(sp3);
        Assert.assertEquals(root.getSize(), sp3.getInventory().getSize());
        Assert.assertNotNull(sp3.getParent());
        Assert.assertEquals(inv2.getPrimaryStorageInstallPath(), sp3.getParent().getInventory().getPrimaryStorageInstallPath());
        sp = dbf.findByUuid(sp3.getUuid(), VolumeSnapshotVO.class);
        Assert.assertEquals(2, sp.getDistance());

        VolumeVO nr = dbf.reload(root);
        Assert.assertEquals(newPath, nr.getInstallPath());
        Assert.assertEquals(100, nr.getActualSize().longValue());
    }
}
