package org.zstack.test.cascade;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.appliancevm.ApplianceVmVO;
import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;

import javax.persistence.Query;
import java.math.BigInteger;
import java.util.concurrent.Callable;

/**
 * 1. create a vm with virtual router
 * 2. delete host
 * 3. set vr migration failure
 * <p>
 * confirm vr is deleted
 */
public class TestCascadeDeletion21 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    KVMSimulatorConfig kconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/virtualRouter/virtualRouterSNAT2.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("VirtualRouterSimulator.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicy.Direct.toString());
        ApplianceVmVO vr = dbf.listAll(ApplianceVmVO.class).get(0);
        String lastHostUuid = vr.getHostUuid();
        kconfig.migrateVmSuccess = false;
        api.deleteHost(lastHostUuid);

        long count = dbf.count(ApplianceVmVO.class);
        Assert.assertEquals(0, count);

        count = new Callable<Long>() {
            @Override
            @Transactional
            public Long call() {
                String sql = "select count(*) from ApplianceVmVO";
                Query q = dbf.getEntityManager().createNativeQuery(sql);
                BigInteger ret = (BigInteger) q.getSingleResult();
                return ret.longValue();
            }
        }.call();
        Assert.assertEquals(0, count);
    }
}
