package org.zstack.test;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.thread.SyncTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.StringDSL.ln;

/**
 */
public class PerfTester {
    Api api;
    ComponentLoader loader;
    int total;
    int syncLevel;
    int timeout;
    CountDownLatch latch;
    ThreadFacade thdf;
    DatabaseFacade dbf;
    List<Long> timeCost = new ArrayList<Long>();

    @Before
    public void setUp() throws Exception {
        total = Integer.valueOf(System.getProperty("total", "500"));
        syncLevel = Integer.valueOf(System.getProperty("syncLevel", "100"));
        timeout = Integer.valueOf(System.getProperty("timeout", "10"));

        BeanConstructor con = new BeanConstructor();
        loader = con.build();
        thdf = loader.getComponent(ThreadFacade.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        api = new Api();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        api.setAdminSession(api.loginAsAdmin());
        final L3NetworkInventory l3 = api.listL3Network(null).get(0);
        final InstanceOfferingInventory ioinv = api.listInstanceOffering(null).get(0);
        final ImageInventory imginv = api.listImage(null).get(0);

        latch = new CountDownLatch(total);
        for (int i = 0; i < total; i++) {
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
                    long start = System.currentTimeMillis();
                    try {
                        VmCreator creator = new VmCreator(api);
                        creator.addL3Network(l3.getUuid());
                        creator.instanceOfferingUuid = ioinv.getUuid();
                        creator.imageUuid = imginv.getUuid();
                        creator.name = "vm-" + finalI;
                        creator.timeout = (int) TimeUnit.MINUTES.toSeconds(10);
                        creator.create();
                        System.out.println(String.format("created %s", creator.name));
                    } finally {
                        timeCost.add(System.currentTimeMillis() - start);
                        latch.countDown();
                    }
                    return null;
                }
            });
        }

        latch.await(timeout, TimeUnit.MINUTES);
        SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
        q.add(VmInstanceVO_.state, Op.EQ, VmInstanceState.Running);
        long count = q.count();
        Assert.assertEquals(total, count);

        long min = 0;
        long max = 0;
        long totalTime = 0;
        for (long t : timeCost) {
            totalTime += t;
            min = Math.min(min, t);
            max = Math.max(max, t);
        }
        long avg = totalTime / timeCost.size();

        String info = ln(
                "Created {0} VMs with parallel level: {1}",
                "Total Time: {2} secs",
                "Max Time: {3} secs",
                "Min Time: {4} secs",
                "Avg Time: {5} secs"
        ).format(total, syncLevel,
                TimeUnit.MILLISECONDS.toMinutes(totalTime),
                TimeUnit.MILLISECONDS.toMinutes(max),
                TimeUnit.MILLISECONDS.toMinutes(min),
                TimeUnit.MILLISECONDS.toMinutes(avg)
        );

        System.out.println(info);
    }
}
