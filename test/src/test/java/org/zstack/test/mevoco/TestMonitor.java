package org.zstack.test.mevoco;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.VmInstance;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.monitoring.*;
import org.zstack.monitoring.MonitorManagerImpl.SetupTimeSeriesMonitorCmd;
import org.zstack.monitoring.MonitorTO.Metric;
import org.zstack.network.service.flat.FlatNetworkServiceSimulatorConfig;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig;
import org.zstack.storage.primary.local.LocalStorageSimulatorConfig.Capacity;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

/**
 * 1. create a vm with mevoco setting
 *
 * confirm vm monitors setup
 * confirm host monitors setup
 *
 * 2. reconnect host
 *
 * confirm host monitors setup
 */
public class TestMonitor {
    CLogger logger = Utils.getLogger(TestMonitor.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LocalStorageSimulatorConfig config;
    FlatNetworkServiceSimulatorConfig fconfig;
    KVMSimulatorConfig kconfig;
    MonitorSimulatorConfig mconfig;
    long totalSize = SizeUnit.GIGABYTE.toByte(100);

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/mevoco/TestMevoco.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("mevoco.xml");
        deployer.addSpringConfig("monitor.xml");
        deployer.addSpringConfig("monitorSimulator.xml");
        deployer.addSpringConfig("localStorage.xml");
        deployer.addSpringConfig("localStorageSimulator.xml");
        deployer.addSpringConfig("flatNetworkServiceSimulator.xml");
        deployer.load();

        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(LocalStorageSimulatorConfig.class);
        fconfig = loader.getComponent(FlatNetworkServiceSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        mconfig = loader.getComponent(MonitorSimulatorConfig.class);

        Capacity c = new Capacity();
        c.total = totalSize;
        c.avail = totalSize;

        config.capacityMap.put("host1", c);

        deployer.build();
        api = deployer.getApi();
        session = api.loginAsAdmin();
    }

    private void checkHost(HostInventory host, boolean refresh) {
        MonitorTO hto = null;
        for (SetupTimeSeriesMonitorCmd cmd : mconfig.setupTimeSeriesMonitorCmdList) {
            for (MonitorTO mto : cmd.monitors) {
                if (mto.getResourceUuid().equals(host.getUuid())) {
                    hto = mto;
                    Assert.assertEquals(refresh, cmd.refresh);
                    break;
                }
            }
        }

        Assert.assertNotNull(hto);
        Assert.assertEquals(HostVO.class.getSimpleName(), hto.getResourceName());
        Assert.assertEquals(MonitorGlobalConfig.HOST_MONITOR_INTERVAL.value(Long.class), Long.valueOf(hto.getInterval()));
        Assert.assertEquals(MonitorGlobalProperty.DB_PUSH_URL, hto.getDbUrl());

        Metric m = CollectionUtils.find(hto.getMetrics(), new Function<Metric, Metric>() {
            @Override
            public Metric call(Metric arg) {
                return arg.getName().equals(MonitorConstants.HOST_CPU_METRIC) ? arg : null;
            }
        });
        Assert.assertNotNull(m);
        Assert.assertTrue(m.getTags().contains(String.format("uuid=%s", host.getUuid())));

        m = CollectionUtils.find(hto.getMetrics(), new Function<Metric, Metric>() {
            @Override
            public Metric call(Metric arg) {
                return arg.getName().equals(MonitorConstants.HOST_DISK_IO_METRIC) ? arg : null;
            }
        });
        Assert.assertNotNull(m);
        Assert.assertTrue(m.getTags().contains(String.format("uuid=%s", host.getUuid())));

        m = CollectionUtils.find(hto.getMetrics(), new Function<Metric, Metric>() {
            @Override
            public Metric call(Metric arg) {
                return arg.getName().equals(MonitorConstants.HOST_MEMORY_METRIC) ? arg : null;
            }
        });
        Assert.assertNotNull(m);
        Assert.assertTrue(m.getTags().contains(String.format("uuid=%s", host.getUuid())));

        m = CollectionUtils.find(hto.getMetrics(), new Function<Metric, Metric>() {
            @Override
            public Metric call(Metric arg) {
                return arg.getName().equals(MonitorConstants.HOST_NETWORK_IO_METRIC) ? arg : null;
            }
        });
        Assert.assertNotNull(m);
        Assert.assertTrue(m.getTags().contains(String.format("uuid=%s", host.getUuid())));
    }
    
	@Test
	public void test() throws InterruptedException, ApiSenderException {
        TimeUnit.SECONDS.sleep(3);

        final VmInstanceInventory vm = deployer.vms.get("TestVm");
        VmNicInventory nic = vm.getVmNics().get(0);
        VolumeInventory root = vm.getRootVolume();

        Assert.assertFalse(mconfig.setupTimeSeriesMonitorCmdList.isEmpty());
        MonitorTO vmto = null;
        for (SetupTimeSeriesMonitorCmd cmd : mconfig.setupTimeSeriesMonitorCmdList) {
            for (MonitorTO to : cmd.monitors) {
                if (to.getResourceUuid().equals(vm.getUuid())) {
                    vmto = to;
                    break;
                }
            }
        }

        Assert.assertNotNull(vmto);
        Assert.assertEquals(VmInstanceVO.class.getSimpleName(), vmto.getResourceName());
        Assert.assertEquals(MonitorGlobalConfig.VM_MONITOR_INTERVAL.value(Long.class), Long.valueOf(vmto.getInterval()));
        Assert.assertEquals(MonitorGlobalProperty.DB_PUSH_URL, vmto.getDbUrl());

        Metric m = CollectionUtils.find(vmto.getMetrics(), new Function<Metric, Metric>() {
            @Override
            public Metric call(Metric arg) {
                return arg.getName().equals(MonitorConstants.VM_CPU_METRIC) ? arg : null;
            }
        });
        Assert.assertNotNull(m);
        Assert.assertTrue(m.getTags().contains(String.format("uuid=%s", vm.getUuid())));

        m = CollectionUtils.find(vmto.getMetrics(), new Function<Metric, Metric>() {
            @Override
            public Metric call(Metric arg) {
                return arg.getName().equals(MonitorConstants.VM_MEMORY_METRIC) ? arg : null;
            }
        });
        Assert.assertNotNull(m);
        Assert.assertTrue(m.getTags().contains(String.format("uuid=%s", vm.getUuid())));

        m = CollectionUtils.find(vmto.getMetrics(), new Function<Metric, Metric>() {
            @Override
            public Metric call(Metric arg) {
                return arg.getName().equals(MonitorConstants.VM_DISK_IO_METRIC) ? arg : null;
            }
        });
        Assert.assertNotNull(m);
        Assert.assertTrue(m.getTags().contains(String.format("uuid=%s", vm.getUuid())));
        Assert.assertTrue(m.getTags().contains(String.format("volumeUuid=%s", root.getUuid())));

        m = CollectionUtils.find(vmto.getMetrics(), new Function<Metric, Metric>() {
            @Override
            public Metric call(Metric arg) {
                return arg.getName().equals(MonitorConstants.VM_NETWORK_IO_METRIC) ? arg : null;
            }
        });
        Assert.assertNotNull(m);
        Assert.assertTrue(m.getTags().contains(String.format("uuid=%s", vm.getUuid())));
        Assert.assertTrue(m.getTags().contains(String.format("nicUuid=%s", nic.getUuid())));

        HostInventory host = deployer.hosts.get("host1");
        checkHost(host, true);

        mconfig.setupTimeSeriesMonitorCmdList.clear();
        api.reconnectHost(host.getUuid());
        TimeUnit.SECONDS.sleep(3);
        checkHost(host, true);
    }
}
