package org.zstack.test.kvm;

import junit.framework.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.kvm.KVMHostFactory;
import org.zstack.kvm.KVMRealizeL2NoVlanNetworkBackend;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

public class TestKVMRealizeL2NoVlanNetworkBackend {
    static CLogger logger = Utils.getLogger(TestAddKvmHost.class);
    static Deployer deployer;
    static Api api;
    static ComponentLoader loader;
    static CloudBus bus;
    static DatabaseFacade dbf;
    static KVMHostFactory kvmFactory;
    static SessionInventory session;
    static KVMSimulatorConfig config;
    static KVMRealizeL2NoVlanNetworkBackend backend;
    boolean success = false;

    @BeforeClass
    public static void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestKVMRealizeL2NoVlanNetworkBackend.xml", con);
        deployer.addSpringConfig("Kvm.xml");
        deployer.addSpringConfig("KVMSimulator.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        kvmFactory = loader.getComponent(KVMHostFactory.class);
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(KVMSimulatorConfig.class);
        backend = loader.getComponent(KVMRealizeL2NoVlanNetworkBackend.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws InterruptedException {
        L2NetworkInventory l2inv = deployer.l2Networks.get("l2novlan");
        HostInventory host = deployer.hosts.get("kvm");
        config.createL2NoVlanNetworkSuccess = true;
        backend.realize(l2inv, host.getUuid(), new Completion(null) {
            @Override
            public void success() {
                success = true;
            }

            @Override
            public void fail(ErrorCode errorCode) {
                success = false;
            }
        });
        TimeUnit.SECONDS.sleep(2);
        Assert.assertTrue(success);
    }
}
