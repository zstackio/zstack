package org.zstack.test.storage.ceph;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.rest.BeforeAsyncJsonPostInterceptor;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.kvm.KVMAgentCommands.AttachIsoCmd;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.ceph.backup.CephBackupStorageSimulatorConfig;
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonVO;
import org.zstack.storage.ceph.primary.CephPrimaryStorageSimulatorConfig;
import org.zstack.storage.ceph.primary.CephPrimaryStorageVO;
import org.zstack.storage.ceph.primary.KvmCephIsoTO;
import org.zstack.storage.ceph.primary.KvmCephIsoTO.MonInfo;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import java.util.concurrent.TimeUnit;

/**
 * test ISO using ceph primary storage
 */
public class TestCeph15 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    CephPrimaryStorageSimulatorConfig config;
    KVMSimulatorConfig kconfig;
    CephBackupStorageSimulatorConfig bconfig;
    RESTFacade restf;
    KvmCephIsoTO isoto;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/ceph/TestCeph15.xml", con);
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
        bconfig = loader.getComponent(CephBackupStorageSimulatorConfig.class);
        restf = loader.getComponent(RESTFacade.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        ImageInventory iso = deployer.images.get("TestIso");
        PrimaryStorageInventory ps = deployer.primaryStorages.get("ceph-pri");

        restf.installBeforeAsyncJsonPostInterceptor(new BeforeAsyncJsonPostInterceptor() {
            @Override
            public void beforeAsyncJsonPost(String url, Object body, TimeUnit unit, long timeout) {
                if (body instanceof AttachIsoCmd) {
                    AttachIsoCmd cmd = (AttachIsoCmd) body;
                    isoto = (KvmCephIsoTO) cmd.iso;
                }
            }

            @Override
            public void beforeAsyncJsonPost(String url, String body, TimeUnit unit, long timeout) {

            }
        });

        api.attachIso(vm.getUuid(), iso.getUuid(), null);
        Assert.assertNotNull(isoto);
        Assert.assertFalse(isoto.getMonInfo().isEmpty());

        CephPrimaryStorageVO ceph = dbf.findByUuid(ps.getUuid(), CephPrimaryStorageVO.class);
        Assert.assertEquals(ceph.getMons().size(), isoto.getMonInfo().size());

        for (final CephPrimaryStorageMonVO mon : ceph.getMons()) {
            MonInfo info = CollectionUtils.find(isoto.getMonInfo(), new Function<MonInfo, MonInfo>() {
                @Override
                public MonInfo call(MonInfo arg) {
                    return arg.getHostname().equals(mon.getHostname()) ? arg : null;
                }
            });

            Assert.assertNotNull(info);
        }
    }
}
