package org.zstack.test.storage.ceph;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.rest.BeforeAsyncJsonPostInterceptor;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.storage.ceph.backup.CephBackupStorageBase;
import org.zstack.storage.ceph.backup.CephBackupStorageBase.GetFactsCmd;
import org.zstack.storage.ceph.backup.CephBackupStorageMonVO;
import org.zstack.storage.ceph.backup.CephBackupStorageSimulatorConfig;
import org.zstack.storage.ceph.primary.CephPrimaryStorageBase;
import org.zstack.storage.ceph.primary.CephPrimaryStorageMonVO;
import org.zstack.storage.ceph.primary.CephPrimaryStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.list;

/**
 * 1. make mons always return different fsid
 * 2. add new mons to existing ceph primary/backup storage
 * <p>
 * confirm the new mons fail to be added
 */
public class TestCeph13 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    CephPrimaryStorageSimulatorConfig pconfig;
    CephBackupStorageSimulatorConfig bconfig;
    KVMSimulatorConfig kconfig;
    RESTFacade restf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/ceph/TestCeph1.xml", con);
        deployer.addSpringConfig("ceph.xml");
        deployer.addSpringConfig("cephSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        pconfig = loader.getComponent(CephPrimaryStorageSimulatorConfig.class);
        bconfig = loader.getComponent(CephBackupStorageSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        restf = loader.getComponent(RESTFacade.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        PrimaryStorageInventory ps = deployer.primaryStorages.get("ceph-pri");
        BackupStorageInventory bs = deployer.backupStorages.get("ceph-bk");

        restf.installBeforeAsyncJsonPostInterceptor(new BeforeAsyncJsonPostInterceptor() {
            @Override
            public void beforeAsyncJsonPost(String url, Object body, TimeUnit unit, long timeout) {
                if (url.contains(CephBackupStorageBase.GET_FACTS)) {
                    GetFactsCmd cmd = (GetFactsCmd) body;
                    bconfig.getFactsCmdFsid.put(cmd.monUuid, Platform.getUuid());
                } else if (url.contains(CephPrimaryStorageBase.GET_FACTS)) {
                    CephPrimaryStorageBase.GetFactsCmd cmd = (CephPrimaryStorageBase.GetFactsCmd) body;
                    pconfig.getFactsCmdFsid.put(cmd.monUuid, Platform.getUuid());
                }
            }

            @Override
            public void beforeAsyncJsonPost(String url, String body, TimeUnit unit, long timeout) {
            }
        });

        api.removeMonFromBackupStorage(bs.getUuid(), list("127.0.0.1", "localhost"));
        api.removeMonFromPrimaryStorage(ps.getUuid(), list("127.0.0.1", "localhost"));

        boolean s = false;
        try {
            api.addMonToCephBackupStorage(bs.getUuid(), list("root:password@127.0.0.1:2222/?monPort=1234", "root1:password1@localhost:3322/?monPort=5678"));
        } catch (ApiSenderException e) {
            s = true;
        }
        Assert.assertTrue(s);
        long count = dbf.count(CephBackupStorageMonVO.class);
        Assert.assertEquals(0, count);

        s = false;
        try {
            api.addMonToCephPrimaryStorage(ps.getUuid(), list("root:password@127.0.0.1:2222/?monPort=1234", "root1:password1@localhost:3322/?monPort=5678"));
        } catch (ApiSenderException e) {
            s = true;
        }
        Assert.assertTrue(s);
        count = dbf.count(CephPrimaryStorageMonVO.class);
        Assert.assertEquals(0, count);
    }
}
