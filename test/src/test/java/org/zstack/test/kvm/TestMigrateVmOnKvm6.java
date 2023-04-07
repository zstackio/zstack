package org.zstack.test.kvm;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.kvm.KVMHostVO;
import org.zstack.kvm.KVMHostVO_;
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

/**
 * 1. add two hosts
 * 2. make two hosts os version mismatch
 * <p>
 * confirm vm migration will fail
 */
public class TestMigrateVmOnKvm6 {
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
        deployer = new Deployer("deployerXml/kvm/TestMigrateVmOnKvm.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(KVMSimulatorConfig.class);
        session = api.loginAsAdmin();
    }

    @Test(expected = ApiSenderException.class)
    public void test() throws ApiSenderException {
        final VmInstanceInventory vm = deployer.vms.get("TestVm");
        HostInventory target = CollectionUtils.find(deployer.hosts.values(), new Function<HostInventory, HostInventory>() {
            @Override
            public HostInventory call(HostInventory arg) {
                if (!arg.getUuid().equals(vm.getHostUuid())) {
                    return arg;
                }
                return null;
            }
        });

        SQL.New(KVMHostVO.class)
                .eq(KVMHostVO_.uuid, target.getUuid())
                .set(KVMHostVO_.osDistribution, "some_fake_distribution")
                .update();

        api.migrateVmInstance(vm.getUuid(), target.getUuid());
    }
}
