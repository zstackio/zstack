package org.zstack.test.securitygroup;

import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.host.HostVO;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.network.securitygroup.RuleTO;
import org.zstack.network.securitygroup.SecurityGroupInventory;
import org.zstack.network.securitygroup.SecurityGroupRuleTO;
import org.zstack.simulator.SimulatorSecurityGroupBackend;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestSecurityGroupRuleRandom {
    static CLogger logger = Utils.getLogger(TestSecurityGroupRuleRandom.class);
    static Deployer deployer;
    static Api api;
    static ComponentLoader loader;
    static DatabaseFacade dbf;
    static SimulatorSecurityGroupBackend sbkd;
    static String[] operations = {"stop", "reboot", "start"};
    static int num = 10;
    CountDownLatch latch = new CountDownLatch(3);

    @BeforeClass
    public static void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/securityGroup/TestApplySeurityGroupRulesToVmOnSimulator2.xml", con);
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        dbf = loader.getComponent(DatabaseFacade.class);
        sbkd = loader.getComponent(SimulatorSecurityGroupBackend.class);
    }

    private String nextOp(VmInstanceState state) {
        Random r = new Random();
        while (true) {
            int i = r.nextInt(3);
            String nextOp = operations[i];
            if (state == VmInstanceState.Running && "start".equals(nextOp)) {
                continue;
            }
            if (state == VmInstanceState.Stopped && "stop".equals(nextOp)) {
                continue;
            }
            if (state == VmInstanceState.Stopped && "reboot".equals(nextOp)) {
                continue;
            }
            return nextOp;
        }
    }

    @AsyncThread
    private void randomOpOnVm(VmInstanceInventory vm) throws ApiSenderException {
        for (int i = 0; i < num; i++) {
            String nextOp = nextOp(VmInstanceState.valueOf(vm.getState()));
            if ("start".equals(nextOp)) {
                vm = api.startVmInstance(vm.getUuid());
            } else if ("stop".equals(nextOp)) {
                vm = api.stopVmInstance(vm.getUuid());
            } else if ("reboot".equals(nextOp)) {
                vm = api.rebootVmInstance(vm.getUuid());
            }
        }

        latch.countDown();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        SecurityGroupInventory scinv = deployer.securityGroups.get("test");
        VmInstanceInventory vm1 = deployer.vms.get("TestVm1");
        VmInstanceInventory vm2 = deployer.vms.get("TestVm2");
        VmInstanceInventory vm3 = deployer.vms.get("TestVm3");

        List<String> nicUuids = new ArrayList<String>();
        nicUuids.add(vm1.getVmNics().get(0).getUuid());
        nicUuids.add(vm2.getVmNics().get(0).getUuid());
        nicUuids.add(vm3.getVmNics().get(0).getUuid());

        api.addVmNicToSecurityGroup(scinv.getUuid(), nicUuids);
        TimeUnit.MILLISECONDS.sleep(500);
        randomOpOnVm(vm1);
        randomOpOnVm(vm2);
        randomOpOnVm(vm3);
        latch.await();
        TimeUnit.SECONDS.sleep(1);

        List<String> internalAllowedIps = new ArrayList<String>();
        List<VmInstanceVO> vmvos = dbf.listAll(VmInstanceVO.class);
        for (VmInstanceVO vmvo : vmvos) {
            internalAllowedIps.add(vmvo.getVmNics().iterator().next().getIp());
        }

    }
}
