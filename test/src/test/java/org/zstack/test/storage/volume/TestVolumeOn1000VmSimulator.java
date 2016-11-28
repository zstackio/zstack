package org.zstack.test.storage.volume;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.thread.SyncTask;
import org.zstack.core.thread.SyncThread;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageVO;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.*;
import org.zstack.identity.IdentityGlobalConfig;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.StringDSL.ln;

public class TestVolumeOn1000VmSimulator {
    CLogger logger = Utils.getLogger(TestVolumeOn1000VmSimulator.class);

    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    ThreadFacade thdf;
    int vmNum = 1000;
    int syncLevel = 1000;
    int timeout = 600;
    volatile List<String> vmUuids = Collections.synchronizedList(new ArrayList<String>());
    List<Long> timeCosts = Collections.synchronizedList(new ArrayList<Long>());
    int volumeNum = 5000;
    CountDownLatch volumeLatch = new CountDownLatch(volumeNum);


    class TimeDetails {
        int vmId;
        long createVolume;
        long attachVolume;
        long total;
    }

    List<TimeDetails> details = Collections.synchronizedList(new ArrayList<TimeDetails>());

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/vm/TestSimulator1000Vm.xml", con);
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        thdf = loader.getComponent(ThreadFacade.class);
        session = api.loginAsAdmin();
    }

    @SyncThread(level = 1000)
    private void attachDataVolume(String vmUuid, String diskOfferingUuid, int vmId) throws ApiSenderException {
        long s = System.currentTimeMillis();
        long cs = 0;
        long ce = 0;
        long as = 0;
        long ae = 0;
        try {
            cs = System.currentTimeMillis();
            ApiSender sender = new ApiSender();
            APICreateDataVolumeMsg msg = new APICreateDataVolumeMsg();
            msg.setSession(api.getAdminSession());
            msg.setName("Data");
            msg.setDiskOfferingUuid(diskOfferingUuid);
            sender.setTimeout((int) TimeUnit.SECONDS.toMillis(600));
            APICreateDataVolumeEvent e = sender.send(msg, APICreateDataVolumeEvent.class);
            VolumeInventory vol = e.getInventory();
            as = ce = System.currentTimeMillis();

            APIAttachDataVolumeToVmMsg msg1 = new APIAttachDataVolumeToVmMsg();
            msg1.setSession(api.getAdminSession());
            msg1.setVmUuid(vmUuid);
            msg1.setVolumeUuid(vol.getUuid());
            sender = new ApiSender();
            sender.setTimeout((int) TimeUnit.SECONDS.toMillis(600));
            sender.send(msg1, APIAttachDataVolumeToVmEvent.class);
            ae = System.currentTimeMillis();
        } finally {
            long e = System.currentTimeMillis();
            timeCosts.add(e - s);

            TimeDetails td = new TimeDetails();
            td.total = TimeUnit.MILLISECONDS.toSeconds(e - s);
            td.createVolume = TimeUnit.MILLISECONDS.toSeconds(ce - cs);
            td.attachVolume = TimeUnit.MILLISECONDS.toSeconds(ae - as);
            td.vmId = vmId;
            details.add(td);

            volumeLatch.countDown();
        }
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        IdentityGlobalConfig.SESSION_TIMEOUT.updateValue(TimeUnit.HOURS.toSeconds(100));
        CoreGlobalProperty.VM_TRACER_ON = false;
        L2NetworkInventory l2 = deployer.l2Networks.get("TestL2Network");
        SimpleQuery<L3NetworkVO> l3q = dbf.createQuery(L3NetworkVO.class);
        l3q.add(L3NetworkVO_.l2NetworkUuid, Op.EQ, l2.getUuid());
        List<L3NetworkVO> l3vos = l3q.list();
        final List<String> l3Uuids = CollectionUtils.transformToList(l3vos, new Function<String, L3NetworkVO>() {
            @Override
            public String call(L3NetworkVO arg) {
                return arg.getUuid();
            }
        });

        final ImageInventory img = deployer.images.get("TestImage");
        ImageVO imgvo = dbf.findByUuid(img.getUuid(), ImageVO.class);
        imgvo.setSize(1);
        dbf.update(imgvo);
        final InstanceOfferingInventory ioinv = deployer.instanceOfferings.get("TestInstanceOffering");
        final Random random = new Random();

        final CountDownLatch latch = new CountDownLatch(vmNum);
        for (int i = 0; i < vmNum; i++) {
            final int finalI = i;
            thdf.syncSubmit(new SyncTask<Object>() {
                @Override
                public String getSyncSignature() {
                    return "creating-vm";
                }

                @Override
                public int getSyncLevel() {
                    return syncLevel;
                }

                @Override
                public String getName() {
                    return getSyncSignature();
                }

                @Override
                public Object call() throws Exception {
                    try {
                        VmCreator creator = new VmCreator(api);
                        creator.addL3Network(l3Uuids.get(random.nextInt(l3Uuids.size())));
                        creator.instanceOfferingUuid = ioinv.getUuid();
                        creator.imageUuid = img.getUuid();
                        creator.name = "vm-" + finalI;
                        creator.timeout = (int) TimeUnit.MINUTES.toSeconds(10);
                        VmInstanceInventory vm = creator.create();
                        vmUuids.add(vm.getUuid());
                    } finally {
                        latch.countDown();
                    }
                    return null;
                }
            });
        }
        latch.await(timeout, TimeUnit.MINUTES);

        System.out.println("start attaching volumes");
        long start = System.currentTimeMillis();

        final DiskOfferingInventory diskOffering = deployer.diskOfferings.get("DataDiskOffering");
        long volumePerVm = volumeNum / vmNum;
        System.out.println(String.format("total %s volumes, %s per vm", volumeNum, volumePerVm));
        for (int j = 0; j < vmNum; j++) {
            String vmUuid = vmUuids.get(j);
            for (long i = 0; i < volumePerVm; i++) {
                attachDataVolume(vmUuid, diskOffering.getUuid(), j);
            }
        }

        volumeLatch.await(timeout, TimeUnit.MINUTES);
        long end = System.currentTimeMillis();

        long max = 0;
        long min = 0;
        long total = 0;
        for (long t : timeCosts) {
            total += t;
            max = Math.max(t, max);
            min = Math.min(t, min);
        }
        long avg = total / timeCosts.size();

        logger.warn(ln(
                "Max time: {0}s",
                "Min time: {1}s",
                "Avg time: {2}s",
                "Total time: {3}s"
        ).format(
                TimeUnit.MILLISECONDS.toSeconds(max),
                TimeUnit.MILLISECONDS.toSeconds(min),
                TimeUnit.MILLISECONDS.toSeconds(avg),
                TimeUnit.MILLISECONDS.toSeconds(end - start)
        ));

        for (TimeDetails td : details) {
            logger.warn(String.format("vm id: %s, total: %s, create time: %s, attach time: %s", td.vmId, td.total, td.createVolume, td.attachVolume));
        }

        SimpleQuery<VolumeVO> q = dbf.createQuery(VolumeVO.class);
        q.add(VolumeVO_.status, Op.EQ, VolumeStatus.Ready);
        q.add(VolumeVO_.type, Op.EQ, VolumeType.Data);
        long count = q.count();
        Assert.assertEquals(volumeNum, count);

    }
}
