package org.zstack.test.securitygroup;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.securitygroup.SecurityGroupInventory;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.storage.backup.sftp.TestSftpBackupStorageDeleteImage2;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * 1. create a security group
 * 2. attach security group to a L3
 * 3. reconnect the KVM host
 * <p>
 * confirm security group is refreshed on the KVM host after host reconnecting
 */
public class TestKvmSecurityGroupRefreshOnReconnect {
    CLogger logger = Utils.getLogger(TestSftpBackupStorageDeleteImage2.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    KVMSimulatorConfig config;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestKvmSecurityGroupRefreshOnReconnect.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(KVMSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        HostInventory host1 = deployer.hosts.get("host1");
        final L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network1");
        SecurityGroupInventory sg = deployer.securityGroups.get("test1");

        VmNicInventory nic = CollectionUtils.find(vm.getVmNics(), new Function<VmNicInventory, VmNicInventory>() {
            @Override
            public VmNicInventory call(VmNicInventory arg) {
                return arg.getL3NetworkUuid().equals(l3.getUuid()) ? arg : null;
            }
        });

        api.addVmNicToSecurityGroup(sg.getUuid(), Arrays.asList(nic.getUuid()));
        TimeUnit.SECONDS.sleep(1);
        config.securityGroupRefreshAllRulesOnHostCmds.clear();
        api.reconnectHost(host1.getUuid());
        // refresh rules on host is async operation
        TimeUnit.SECONDS.sleep(1);
        Assert.assertEquals(1, config.securityGroupRefreshAllRulesOnHostCmds.size());
    }
}
