package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.host.HostGlobalConfig;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.host.HostInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmTracerCanonicalEvents;
import org.zstack.header.vm.VmTracerCanonicalEvents.StrangerVmFoundData;
import org.zstack.simulator.SimulatorController;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 1. create a stranger vm
 * <p>
 * confirm the stranger vm event is fired
 */
@Deprecated
public class TestVmStateTracer2 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    GlobalConfigFacade gcf;
    SimulatorController sctrl;
    boolean isSuccess = false;
    EventFacade evtf;
    String strangerVmUuid;
    String hostUuidStrangerVmOn;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestVmStateTracer.xml");
        loader = deployer.getComponentLoader();
        gcf = loader.getComponent(GlobalConfigFacade.class);
        sctrl = loader.getComponent(SimulatorController.class);
        HostGlobalConfig.PING_HOST_INTERVAL.updateValue(1);
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        evtf = loader.getComponent(EventFacade.class);

        deployer.build();
        api = deployer.getApi();
    }

    @Test
    public void test() throws InterruptedException, ApiSenderException {
        strangerVmUuid = Platform.getUuid();
        HostInventory hinv = api.listHosts(null).get(0);
        hostUuidStrangerVmOn = hinv.getUuid();
        evtf.on(VmTracerCanonicalEvents.STRANGER_VM_FOUND_PATH, new EventCallback() {
            @Override
            public void run(Map tokens, Object data) {
                StrangerVmFoundData sd = (StrangerVmFoundData) data;
                if (sd.getVmIdentity().equals(strangerVmUuid) && sd.getVmState() == VmInstanceState.Running &&
                        hostUuidStrangerVmOn.equals(sd.getHostUuid())) {
                    isSuccess = true;
                }
            }
        });

        sctrl.setVmStateOnSimulatorHost(hostUuidStrangerVmOn, strangerVmUuid, VmInstanceState.Running);
        TimeUnit.SECONDS.sleep(3);
        Assert.assertTrue(isSuccess);
    }
}
