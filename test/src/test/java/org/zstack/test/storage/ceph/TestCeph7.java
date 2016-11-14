package org.zstack.test.storage.ceph;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.host.HostGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.ceph.primary.CephPrimaryStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

import java.util.concurrent.TimeUnit;

/**
 * 1. use ceph for backup storage and primary storage
 * 2. attach the ps to the kvm cluster
 * <p>
 * confirm the kvm secret created
 * <p>
 * 3. reconnect the kvm host
 * <p>
 * confirm the kvm secret created
 * <p>
 * 4. make the kvm host disconnected
 * 5. change check interval to 1s
 * 6. wait 3s
 * <p>
 * confirm the kvm secret created
 */
public class TestCeph7 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    CephPrimaryStorageSimulatorConfig config;
    KVMSimulatorConfig kconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/ceph/TestCeph7.xml", con);
        deployer.addSpringConfig("ceph.xml");
        deployer.addSpringConfig("cephSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(CephPrimaryStorageSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        PrimaryStorageInventory pri = deployer.primaryStorages.get("ceph-pri");
        ClusterInventory cluster = deployer.clusters.get("Cluster1");
        HostInventory host = deployer.hosts.get("host1");

        api.attachPrimaryStorage(cluster.getUuid(), pri.getUuid());
        Assert.assertFalse(config.createKvmSecretCmds.isEmpty());

        config.createEmptyVolumeCmds.clear();
        api.reconnectHost(host.getUuid());
        Assert.assertFalse(config.createKvmSecretCmds.isEmpty());

        HostGlobalConfig.PING_HOST_INTERVAL.updateValue(1);
        kconfig.pingSuccess = false;
        config.createEmptyVolumeCmds.clear();
        TimeUnit.SECONDS.sleep(3);
        kconfig.pingSuccess = true;
        TimeUnit.SECONDS.sleep(3);
        HostVO hvo = dbf.findByUuid(host.getUuid(), HostVO.class);
        Assert.assertEquals(HostStatus.Connected, hvo.getStatus());
        Assert.assertFalse(config.createKvmSecretCmds.isEmpty());
    }
}
