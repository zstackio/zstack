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
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.AbstractBeforeSendMessageInterceptor;
import org.zstack.header.message.Message;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.primary.ImageCacheVO_;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotTree;
import org.zstack.header.storage.snapshot.VolumeSnapshotTree.SnapshotLeaf;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.hotfix.HotFix1169Result;
import org.zstack.kvm.KvmRunShellMsg;
import org.zstack.kvm.KvmRunShellReply;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageKvmBackend.CacheInstallPath;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.function.Function;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class TestKvmHotfix1169_8 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;
    KVMSimulatorConfig kconfig;
    long totalSize = SizeUnit.GIGABYTE.toByte(100);

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestKvmHostFix1169_8.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("localStorageSimulator.xml");
        deployer.addSpringConfig("localStorage.xml");
        deployer.addSpringConfig("hotfix.xml");
        deployer.load();

        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(LocalStorageSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);

        Capacity c = new Capacity();
        c.total = totalSize;
        c.avail = totalSize;

        config.capacityMap.put("host1", c);
        config.capacityMap.put("host2", c);

        deployer.build();
        api = deployer.getApi();
        session = api.loginAsAdmin();
    }
    
	@Test
	public void test() throws ApiSenderException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        String hostUuid1 = vm.getHostUuid();
        api.stopVmInstance(vm.getUuid());

        VmInstanceInventory vm2 = deployer.vms.get("TestVm1");
        String hostUuid2 = vm2.getHostUuid();
        VolumeInventory data = vm2.getAllVolumes().stream().filter(d->d.getType().equals(VolumeType.Data.toString())).findAny().get();
        api.destroyVmInstance(vm2.getUuid());

        VolumeSnapshotInventory inv1 = api.createSnapshot(vm.getRootVolumeUuid());
        VolumeSnapshotInventory inv2 = api.createSnapshot(vm.getRootVolumeUuid());

        VolumeVO root = dbf.findByUuid(vm.getRootVolumeUuid(), VolumeVO.class);
        ImageInventory image = deployer.images.get("TestImage");
        SimpleQuery<ImageCacheVO> iq = dbf.createQuery(ImageCacheVO.class);
        iq.add(ImageCacheVO_.imageUuid, Op.EQ, image.getUuid());
        iq.add(ImageCacheVO_.installUrl, Op.LIKE, String.format("%%%s%%", hostUuid1));
        ImageCacheVO ic = iq.find();

        CacheInstallPath path = new CacheInstallPath();
        path.fullPath = ic.getInstallUrl();
        path.disassemble();
        String cachePath = path.installPath;

        List<String> treeOnStorage = new ArrayList<>();
        // info for the root volume of vm1
        treeOnStorage.add(String.format("%s %s %s %s", inv1.getPrimaryStorageInstallPath(), cachePath,
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

        List<String> treeOnStorage2 = new ArrayList<>();
        // info for the data volume of vm2
        treeOnStorage2.add(String.format("%s %s %s %s", data.getInstallPath(), "NONE",
                data.getSize(), 0));
        String newPath1 = PathUtil.join(new File(data.getInstallPath()).getParent(), "abcd.qcow2");
        treeOnStorage2.add(String.format("%s %s %s %s", newPath1, data.getInstallPath(),
                100, 0));


        bus.installBeforeSendMessageInterceptor(new AbstractBeforeSendMessageInterceptor() {
            @Override
            public void intercept(Message msg) {
                if (msg instanceof KvmRunShellMsg) {
                    bus.makeLocalServiceId(msg, TestKvmHotfix1169_1.class.getName());
                }
            }
        }, KvmRunShellMsg.class);

        bus.registerService(new AbstractService() {
            @Override
            public void handleMessage(Message msg) {
                if (msg instanceof KvmRunShellMsg) {
                    KvmRunShellMsg kmsg = (KvmRunShellMsg) msg;
                    KvmRunShellReply reply = new KvmRunShellReply();
                    if (kmsg.getHostUuid().equals(hostUuid1)) {
                        reply.setReturnCode(0);
                        reply.setStdout(StringUtils.join(treeOnStorage, "\n"));
                    } else {
                        reply.setReturnCode(0);
                        reply.setStdout(StringUtils.join(treeOnStorage2, "\n"));
                    }
                    bus.reply(msg, reply);
                } else {
                    bus.dealWithUnknownMessage(msg);
                }
            }

            @Override
            public String getId() {
                return bus.makeLocalServiceId(TestKvmHotfix1169_1.class.getName());
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
        Assert.assertEquals(2, results.size());

        HotFix1169Result res = results.stream().filter(r->r.volumeUuid.equals(root.getUuid())).findAny().get();
        Assert.assertTrue(res.success);
        Assert.assertEquals(root.getUuid(), res.volumeUuid);
        Assert.assertEquals(root.getName(), res.volumeName);

        SimpleQuery<VolumeSnapshotVO> q = dbf.createQuery(VolumeSnapshotVO.class);
        q.add(VolumeSnapshotVO_.volumeUuid, Op.EQ, root.getUuid());
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


        // for the data volume
        q = dbf.createQuery(VolumeSnapshotVO.class);
        q.add(VolumeSnapshotVO_.volumeUuid, Op.EQ, data.getUuid());
        sps = q.list();

        Assert.assertEquals(1, sps.size());
        tree = VolumeSnapshotTree.fromVOs(sps);
        sp1 = tree.findSnapshot(new Function<Boolean, VolumeSnapshotInventory>() {
            @Override
            public Boolean call(VolumeSnapshotInventory arg) {
                return arg.getPrimaryStorageInstallPath().equals(data.getInstallPath());
            }
        });
        Assert.assertNotNull(sp1);
        Assert.assertEquals(data.getSize(), sp1.getInventory().getSize());
        Assert.assertNull(sp1.getParent());
        sp = dbf.findByUuid(sp1.getUuid(), VolumeSnapshotVO.class);
        Assert.assertEquals(0, sp.getDistance());

        VolumeVO d = dbf.findByUuid(data.getUuid(), VolumeVO.class);
        Assert.assertEquals(newPath1, d.getInstallPath());
        Assert.assertEquals(100, d.getActualSize().longValue());
    }
}
