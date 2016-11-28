package org.zstack.test.virtualrouter;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.SyncTask;
import org.zstack.core.thread.SyncThread;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.SimpleFlowChain;
import org.zstack.core.workflow.WorkFlowStatistic;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageVO;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.rest.HttpCallStatistic;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.network.service.portforwarding.PortForwardingProtocolType;
import org.zstack.network.service.portforwarding.PortForwardingRuleInventory;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.simulator.SimulatorGlobalProperty;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.StringDSL.ln;

public class TestVirtualRouterPortForwarding1000Vm {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    VirtualRouterSimulatorConfig vconfig;
    KVMSimulatorConfig kconfig;
    ThreadFacade thdf;
    RESTFacade restf;
    int total = 1000;
    int syncLevel = 150;
    int timeout = 600;
    int ruleNum = 5000;
    CountDownLatch ruleLatch = new CountDownLatch(ruleNum);
    List<Long> timeCost = new ArrayList<Long>(ruleNum);
    List<Long> vipCost = new ArrayList<Long>(ruleNum);
    List<Long> createCost = new ArrayList<Long>(ruleNum);
    final List<String> vmNicUuids = new ArrayList<String>(total);

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/virtualRouter/TestPortForwarding1000Vm.xml", con);
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("VirtualRouterSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("PortForwarding.xml");
        deployer.addSpringConfig("vip.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        vconfig = loader.getComponent(VirtualRouterSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        thdf = loader.getComponent(ThreadFacade.class);
        restf = loader.getComponent(RESTFacade.class);
        session = api.loginAsAdmin();
        SimulatorGlobalProperty.NOT_CACHE_AGENT_COMMAND = true;
    }

    @SyncThread(level = 1000)
    private void createRule(L3NetworkInventory pub, L3NetworkInventory guest, String vmNicUuid, int port) throws ApiSenderException {
        try {
            api.setTimeout(600);
            long s = System.currentTimeMillis();
            VipInventory vip = api.acquireIp(pub.getUuid());
            long s1 = System.currentTimeMillis();
            vipCost.add(s1 - s);
            PortForwardingRuleInventory rule = new PortForwardingRuleInventory();
            rule.setVipUuid(vip.getUuid());
            rule.setName("pf");
            rule.setVipPortStart(port);
            rule.setVipPortEnd(port);
            rule.setPrivatePortStart(port);
            rule.setPrivatePortEnd(port);
            rule.setProtocolType(PortForwardingProtocolType.TCP.toString());
            rule = api.createPortForwardingRuleByFullConfig(rule);
            long s2 = System.currentTimeMillis();
            createCost.add(s2 - s1);
            api.attachPortForwardingRule(rule.getUuid(), vmNicUuid);
            timeCost.add(System.currentTimeMillis() - s2);
        } finally {
            ruleLatch.countDown();
        }
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        CoreGlobalProperty.VM_TRACER_ON = false;
        final L3NetworkInventory guestL3 = deployer.l3Networks.get("TestL3Network1");
        L3NetworkInventory pubL3 = deployer.l3Networks.get("MgmtNetwork");

        final ImageInventory img = deployer.images.get("TestImage");
        ImageVO imgvo = dbf.findByUuid(img.getUuid(), ImageVO.class);
        imgvo.setSize(1);
        dbf.update(imgvo);
        final InstanceOfferingInventory ioinv = deployer.instanceOfferings.get("TestInstanceOffering");

        final CountDownLatch latch = new CountDownLatch(total);
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
                    try {
                        VmCreator creator = new VmCreator(api);
                        creator.addL3Network(guestL3.getUuid());
                        creator.instanceOfferingUuid = ioinv.getUuid();
                        creator.imageUuid = img.getUuid();
                        creator.name = "vm-" + finalI;
                        creator.timeout = (int) TimeUnit.MINUTES.toSeconds(10);
                        VmInstanceInventory vm = creator.create();
                        synchronized (vmNicUuids) {
                            vmNicUuids.add(vm.getVmNics().get(0).getUuid());
                        }
                    } finally {
                        latch.countDown();
                    }
                    return null;
                }
            });
        }

        latch.await(timeout, TimeUnit.MINUTES);

        CoreGlobalProperty.PROFILER_WORKFLOW = true;
        CoreGlobalProperty.PROFILER_HTTP_CALL = true;
        SimulatorGlobalProperty.NOT_CACHE_AGENT_COMMAND = true;
        long start = System.currentTimeMillis();
        long rulePerVm = ruleNum / total;

        System.out.println(String.format("start creating port forwarding rule, total: %s, rule per vm: %s", ruleNum, rulePerVm));
        for (int j = 0; j < total; j++) {
            String nicUuid = vmNicUuids.get(j);
            for (int i = 0; i < rulePerVm; i++) {
                createRule(pubL3, guestL3, nicUuid, i);
            }
        }

        ruleLatch.await(timeout, TimeUnit.MINUTES);
        long end = System.currentTimeMillis();

        long min = 0;
        long max = 0;
        long avg = 0;
        long total = 0;
        for (long t : vipCost) {
            min = Math.min(t, min);
            max = Math.max(t, max);
            total += t;
        }
        avg = total / vipCost.size();

        System.out.println(ln(
                "create vip cost:",
                "total time: {0}",
                "max: {1}",
                "min: {2}",
                "avg: {3}"
        ).format(
                TimeUnit.MILLISECONDS.toSeconds(end - start),
                TimeUnit.MILLISECONDS.toSeconds(max),
                TimeUnit.MILLISECONDS.toSeconds(min),
                TimeUnit.MILLISECONDS.toSeconds(avg)
        ));

        min = 0;
        max = 0;
        avg = 0;
        total = 0;
        for (long t : createCost) {
            min = Math.min(t, min);
            max = Math.max(t, max);
            total += t;
        }
        avg = total / createCost.size();

        System.out.println(ln(
                "create pf cost:",
                "total time: {0}",
                "max: {1}",
                "min: {2}",
                "avg: {3}"
        ).format(
                TimeUnit.MILLISECONDS.toSeconds(end - start),
                TimeUnit.MILLISECONDS.toSeconds(max),
                TimeUnit.MILLISECONDS.toSeconds(min),
                TimeUnit.MILLISECONDS.toSeconds(avg)
        ));

        min = 0;
        max = 0;
        avg = 0;
        total = 0;
        for (long t : timeCost) {
            min = Math.min(t, min);
            max = Math.max(t, max);
            total += t;
        }
        avg = total / timeCost.size();

        System.out.println(ln(
                "attach pf cost:",
                "total time: {0}",
                "max: {1}",
                "min: {2}",
                "avg: {3}"
        ).format(
                TimeUnit.MILLISECONDS.toSeconds(end - start),
                TimeUnit.MILLISECONDS.toSeconds(max),
                TimeUnit.MILLISECONDS.toSeconds(min),
                TimeUnit.MILLISECONDS.toSeconds(avg)
        ));

        List<WorkFlowStatistic> stats = new ArrayList<WorkFlowStatistic>();
        stats.addAll(SimpleFlowChain.getStatistics().values());
        Collections.sort(stats, new Comparator<WorkFlowStatistic>() {
            @Override
            public int compare(WorkFlowStatistic o1, WorkFlowStatistic o2) {
                return (int) (o2.getTotalTime() - o1.getTotalTime());
            }
        });
        for (WorkFlowStatistic stat : stats) {
            System.out.println(stat.toString());
        }
        System.out.println();

        List<HttpCallStatistic> hstats = new ArrayList<HttpCallStatistic>();
        hstats.addAll(restf.getStatistics().values());
        Collections.sort(hstats, new Comparator<HttpCallStatistic>() {
            @Override
            public int compare(HttpCallStatistic o1, HttpCallStatistic o2) {
                return (int) (o2.getTotalTime() - o1.getTotalTime());
            }
        });
        for (HttpCallStatistic stat : hstats) {
            System.out.println(stat.toString());
        }
    }
}
