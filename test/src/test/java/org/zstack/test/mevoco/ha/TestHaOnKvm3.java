package org.zstack.test.mevoco.ha;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.host.HostGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.ha.*;
import org.zstack.ha.HaKvmHostSiblingChecker.ScanCmd;
import org.zstack.header.host.HostCanonicalEvents;
import org.zstack.header.host.HostCanonicalEvents.HostStatusChangedData;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.ReconnectHostMsg;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.AbstractBeforeDeliveryMessageInterceptor;
import org.zstack.header.message.Message;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 1. make the host where the VM runs down
 * 2. make the scan command returns unstable
 * <p>
 * confirm the VM is not HA started on another host
 */

public class TestHaOnKvm3 {
    CLogger logger = Utils.getLogger(TestHaOnKvm3.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    KVMSimulatorConfig config;
    HaKvmSimulatorConfig hconfig;
    EventFacade evtf;

    boolean success1 = false;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/ha/TestHaOnKvm1.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("ha.xml");
        deployer.addSpringConfig("haSimulator.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        evtf = loader.getComponent(EventFacade.class);
        config = loader.getComponent(KVMSimulatorConfig.class);
        hconfig = loader.getComponent(HaKvmSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        HaGlobalConfig.ALL.updateValue(true);
        HaGlobalConfig.HOST_CHECK_INTERVAL.updateValue(1);
        HaGlobalConfig.HOST_CHECK_MAX_ATTEMPTS.updateValue(3);
        HaGlobalConfig.HOST_CHECK_SUCCESS_INTERVAL.updateValue(1);
        HaGlobalConfig.HOST_CHECK_SUCCESS_TIMES.updateValue(3);
        HostGlobalConfig.AUTO_RECONNECT_ON_ERROR.updateValue(false);
        HostGlobalConfig.PING_HOST_INTERVAL.updateValue(1);

        final HostInventory host1 = deployer.hosts.get("host1");
        HostInventory host2 = deployer.hosts.get("host2");

        final VmInstanceInventory vm = deployer.vms.get("TestVm");
        api.setVmHaLevel(vm.getUuid(), VmHaLevel.OnHostFailure, null);
        String level = HaSystemTags.HA.getTokenByResourceUuid(vm.getUuid(), HaSystemTags.HA_TOKEN);
        Assert.assertEquals(VmHaLevel.OnHostFailure.toString(), level);

        evtf.on(HostCanonicalEvents.HOST_STATUS_CHANGED_PATH, new EventCallback() {
            @Override
            public void run(Map tokens, Object data) {
                // once the host is disconnected, make the sequent ping success
                HostStatusChangedData d = (HostStatusChangedData) data;
                config.pingSuccessMap.put(host1.getUuid(), true);
            }
        });

        bus.installBeforeDeliveryMessageInterceptor(new AbstractBeforeDeliveryMessageInterceptor() {
            @Override
            public void intercept(Message msg) {
                if (msg instanceof ReconnectHostMsg) {
                    success1 = true;
                }
            }
        }, ReconnectHostMsg.class);

        hconfig.scanResult = HaKvmHostSiblingChecker.RET_NOT_STABLE;
        config.pingSuccessMap.put(host1.getUuid(), false);
        TimeUnit.SECONDS.sleep(5);

        VmInstanceVO vmvo = dbf.findByUuid(vm.getUuid(), VmInstanceVO.class);
        Assert.assertEquals(VmInstanceState.Running, vmvo.getState());
        Assert.assertEquals(host1.getUuid(), vmvo.getHostUuid());
        Assert.assertEquals(1, hconfig.scanCmds.size());
        ScanCmd cmd = hconfig.scanCmds.get(0);
        Assert.assertEquals(HaGlobalConfig.HOST_CHECK_INTERVAL.value(Long.class).longValue(), cmd.interval);
        Assert.assertEquals(HaGlobalConfig.HOST_CHECK_MAX_ATTEMPTS.value(Integer.class).intValue(), cmd.times);
        Assert.assertEquals(HaGlobalConfig.HOST_CHECK_SUCCESS_INTERVAL.value(Long.class).longValue(), cmd.successInterval);
        Assert.assertEquals(HaGlobalConfig.HOST_CHECK_SUCCESS_TIMES.value(Integer.class).intValue(), cmd.times);
    }
}
