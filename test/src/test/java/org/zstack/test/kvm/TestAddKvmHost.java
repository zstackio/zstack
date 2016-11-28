package org.zstack.test.kvm;

import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.HostCapacityVO;
import org.zstack.header.allocator.HostCpuOverProvisioningManager;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.host.APIAddHostEvent;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.kvm.APIAddKVMHostMsg;
import org.zstack.kvm.KVMHostFactory;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

public class TestAddKvmHost {
    static CLogger logger = Utils.getLogger(TestAddKvmHost.class);
    static Deployer deployer;
    static Api api;
    static ComponentLoader loader;
    static CloudBus bus;
    static DatabaseFacade dbf;
    static KVMHostFactory kvmFactory;
    static SessionInventory session;
    static KVMSimulatorConfig config;
    static HostCpuOverProvisioningManager cpuMgr;

    @BeforeClass
    public static void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestAddKvmHost.xml", con);
        deployer.addSpringConfig("Kvm.xml");
        deployer.addSpringConfig("KVMSimulator.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        kvmFactory = loader.getComponent(KVMHostFactory.class);
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(KVMSimulatorConfig.class);
        cpuMgr = loader.getComponent(HostCpuOverProvisioningManager.class);
        session = api.loginAsAdmin();
    }

    private HostInventory addHost() throws ApiSenderException {
        ClusterInventory cinv = api.listClusters(null).get(0);
        APIAddKVMHostMsg msg = new APIAddKVMHostMsg();
        msg.setName("KVM-1");
        msg.setClusterUuid(cinv.getUuid());
        msg.setManagementIp("localhost");
        msg.setUsername("admin");
        msg.setPassword("password");
        msg.setSession(session);
        ApiSender sender = api.getApiSender();
        APIAddHostEvent evt = sender.send(msg, APIAddHostEvent.class);
        return evt.getInventory();
    }

    @Test
    public void test() throws ApiSenderException {
        config.connectSuccess = true;
        config.connectException = false;
        config.hostFactSuccess = true;
        config.hostFactException = false;
        config.cpuNum = 1;
        config.cpuSpeed = 2600;
        config.totalMemory = SizeUnit.GIGABYTE.toByte(8);
        config.usedMemory = SizeUnit.MEGABYTE.toByte(512);
        String huuid = addHost().getUuid();
        HostCapacityVO hvo = dbf.findByUuid(huuid, HostCapacityVO.class);
        Assert.assertEquals(cpuMgr.calculateHostCpuByRatio(huuid, (int) config.cpuNum), hvo.getTotalCpu());
        Assert.assertEquals(config.usedMemory, hvo.getUsedPhysicalMemory());
        Assert.assertEquals(config.totalMemory, hvo.getTotalMemory());
    }
}
