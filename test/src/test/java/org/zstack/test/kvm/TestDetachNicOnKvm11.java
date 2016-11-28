package org.zstack.test.kvm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l3.IpRangeInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
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
import org.zstack.utils.network.NetworkUtils;

/**
 * 1. set static ip to a vm nic
 * 2. detach the nic
 * <p>
 * confirm static ip tag is removed
 */
public class TestDetachNicOnKvm11 {
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
        deployer = new Deployer("deployerXml/kvm/TestCreateVmOnKvm.xml", con);
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
        final L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network1");
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        VmNicInventory nic = CollectionUtils.find(vm.getVmNics(), new Function<VmNicInventory, VmNicInventory>() {
            @Override
            public VmNicInventory call(VmNicInventory arg) {
                return arg.getL3NetworkUuid().equals(l3.getUuid()) ? arg : null;
            }
        });

        IpRangeInventory ipr = l3.getIpRanges().get(0);
        long s = NetworkUtils.ipv4StringToLong(ipr.getStartIp());
        long e = NetworkUtils.ipv4StringToLong(ipr.getEndIp());
        String targetIp = null;
        for (long i = s; s < e; s++) {
            if (i != NetworkUtils.ipv4StringToLong(nic.getIp())) {
                targetIp = NetworkUtils.longToIpv4String(i);
                break;
            }
        }
        api.stopVmInstance(vm.getUuid());
        api.setStaticIp(vm.getUuid(), l3.getUuid(), targetIp);
        api.detachNic(nic.getUuid());
        boolean success = VmSystemTags.STATIC_IP.hasTag(vm.getUuid());
        Assert.assertFalse(success);
    }
}
