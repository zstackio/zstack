package org.zstack.test.kvm;

import junit.framework.Assert;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmGlobalConfig;
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
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.hotfix.HotFix1169Result;
import org.zstack.kvm.KvmRunShellMsg;
import org.zstack.kvm.KvmRunShellReply;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/*
* take snapshot from vm's root volume
*/
public class TestKvmHotfix1169_5 {
    CLogger logger = Utils.getLogger(TestKvmHotfix1169_5.class);
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
        deployer = new Deployer("deployerXml/kvm/TestKvmHostFix1169_4.xml", con);
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
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VolumeInventory data = vm.getAllVolumes().stream().filter(d->d.getType().equals(VolumeType.Data.toString())).findAny().get();

        String volUuid = data.getUuid();
        VolumeVO dataVolume = dbf.findByUuid(volUuid, VolumeVO.class);

        VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicy.Direct.toString());
        api.destroyVmInstance(vm.getUuid());

        List<String> treeOnStorage = new ArrayList<>();
        treeOnStorage.add(String.format("%s %s %s %s", data.getInstallPath(), "NONE",
                data.getSize(), 0));
        String newPath1 = PathUtil.join(new File(data.getInstallPath()).getParent(), "1", "abcd.qcow2");
        treeOnStorage.add(String.format("%s %s %s %s", newPath1, data.getInstallPath(),
                100, 0));
        String newPath2 = PathUtil.join(new File(data.getInstallPath()).getParent(), "2", "abcd.qcow2");
        treeOnStorage.add(String.format("%s %s %s %s", newPath2, newPath1,
                200, 0));
        // the new volume
        String newPath3 = PathUtil.join(new File(data.getInstallPath()).getParent(), "3", "abcd.qcow2");
        treeOnStorage.add(String.format("%s %s %s %s", newPath3, newPath2,
                300, 0));

        bus.installBeforeSendMessageInterceptor(new AbstractBeforeSendMessageInterceptor() {
            @Override
            public void intercept(Message msg) {
                if (msg instanceof KvmRunShellMsg) {
                    bus.makeLocalServiceId(msg, TestKvmHotfix1169_5.class.getName());
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
                return bus.makeLocalServiceId(TestKvmHotfix1169_5.class.getName());
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

        List<HotFix1169Result> results = api.hotfix1169(dataVolume.getPrimaryStorageUuid());
        Assert.assertEquals(1, results.size());

        HotFix1169Result res = results.get(0);
        Assert.assertTrue(res.success);
        Assert.assertEquals(dataVolume.getUuid(), res.volumeUuid);
        Assert.assertEquals(dataVolume.getName(), res.volumeName);

        SimpleQuery<VolumeSnapshotVO> q = dbf.createQuery(VolumeSnapshotVO.class);
        q.add(VolumeSnapshotVO_.volumeUuid, Op.EQ, dataVolume.getUuid());
        List<VolumeSnapshotVO> sps = q.list();

        Assert.assertEquals(3, sps.size());
        VolumeSnapshotTree tree = VolumeSnapshotTree.fromVOs(sps);
        SnapshotLeaf sp1 = tree.findSnapshot(new Function<Boolean, VolumeSnapshotInventory>() {
            @Override
            public Boolean call(VolumeSnapshotInventory arg) {
                return arg.getPrimaryStorageInstallPath().equals(data.getInstallPath());
            }
        });
        Assert.assertNotNull(sp1);
        Assert.assertEquals(data.getSize(), sp1.getInventory().getSize());
        Assert.assertNull(sp1.getParent());
        VolumeSnapshotVO sp = dbf.findByUuid(sp1.getUuid(), VolumeSnapshotVO.class);
        Assert.assertEquals(0, sp.getDistance());

        SnapshotLeaf sp2 = tree.findSnapshot(new Function<Boolean, VolumeSnapshotInventory>() {
            @Override
            public Boolean call(VolumeSnapshotInventory arg) {
                return arg.getPrimaryStorageInstallPath().equals(newPath1);
            }
        });
        Assert.assertNotNull(sp2);
        Assert.assertEquals(100, sp2.getInventory().getSize());
        Assert.assertNotNull(sp2.getParent());
        Assert.assertEquals(data.getInstallPath(), sp2.getParent().getInventory().getPrimaryStorageInstallPath());
        sp = dbf.findByUuid(sp2.getUuid(), VolumeSnapshotVO.class);
        Assert.assertEquals(1, sp.getDistance());

        SnapshotLeaf sp3 = tree.findSnapshot(new Function<Boolean, VolumeSnapshotInventory>() {
            @Override
            public Boolean call(VolumeSnapshotInventory arg) {
                return arg.getPrimaryStorageInstallPath().equals(newPath2);
            }
        });
        Assert.assertNotNull(sp3);
        Assert.assertEquals(200, sp3.getInventory().getSize());
        Assert.assertNotNull(sp3.getParent());
        Assert.assertEquals(newPath1, sp3.getParent().getInventory().getPrimaryStorageInstallPath());
        sp = dbf.findByUuid(sp3.getUuid(), VolumeSnapshotVO.class);
        Assert.assertEquals(2, sp.getDistance());

        VolumeVO nr = dbf.findByUuid(data.getUuid(), VolumeVO.class);
        Assert.assertEquals(newPath3, nr.getInstallPath());
        Assert.assertEquals(300, nr.getActualSize().longValue());
    }
}
