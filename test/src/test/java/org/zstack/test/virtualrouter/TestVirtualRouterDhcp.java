package org.zstack.test.virtualrouter;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.DhcpInfo;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * 1. create a vm
 * 2. stop vr
 * 3. stop vm
 *
 * confirm vm stopped successfully
 */
public class TestVirtualRouterDhcp {
	CLogger logger = Utils.getLogger(TestVirtualRouterDhcp.class);
	Deployer deployer;
	Api api;
	ComponentLoader loader;
	CloudBus bus;
	DatabaseFacade dbf;
	SessionInventory session;
	VirtualRouterSimulatorConfig vconfig;
	KVMSimulatorConfig kconfig;

	@Before
	public void setUp() throws Exception {
		DBUtil.reDeployDB();
		WebBeanConstructor con = new WebBeanConstructor();
		deployer = new Deployer("deployerXml/virtualRouter/TestVirtualRouterDhcp.xml", con);
        deployer.addSpringConfig("NetworkService.xml");
		deployer.addSpringConfig("VirtualRouter.xml");
		deployer.addSpringConfig("VirtualRouterSimulator.xml");
		deployer.addSpringConfig("KVMRelated.xml");
		deployer.build();
		api = deployer.getApi();
		loader = deployer.getComponentLoader();
		vconfig = loader.getComponent(VirtualRouterSimulatorConfig.class);
		kconfig = loader.getComponent(KVMSimulatorConfig.class);
		bus = loader.getComponent(CloudBus.class);
		dbf = loader.getComponent(DatabaseFacade.class);
		session = api.loginAsAdmin();
	}

	@Test
	public void test() throws ApiSenderException, InterruptedException {
        final VmInstanceInventory vm = deployer.vms.get("TestVm");
        VmNicInventory defaultNic = CollectionUtils.find(vm.getVmNics(), new Function<VmNicInventory, VmNicInventory>() {
            @Override
            public VmNicInventory call(VmNicInventory arg) {
                return arg.getL3NetworkUuid().equals(vm.getDefaultL3NetworkUuid()) ? arg : null;
            }
        });

        Assert.assertNotNull(defaultNic);
        Assert.assertEquals(2, vconfig.dhcpInfoMap.size());

        final List<String> l3s = CollectionUtils.transformToList(vm.getVmNics(), new Function<String, VmNicInventory>() {
            @Override
            public String call(VmNicInventory arg) {
                return arg.getL3NetworkUuid();
            }
        });

        List<String> vrPrivateIps = new Callable<List<String>>() {
            @Override
            @Transactional(readOnly = true)
            public List<String> call() {
                String sql = "select nic.ip from VmNicVO nic, ApplianceVmVO apvm where nic.vmInstanceUuid = apvm.uuid and nic.l3NetworkUuid in (:l3Uuids)";
                TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                q.setParameter("l3Uuids", l3s);
                return q.getResultList();
            }
        }.call();

        boolean success = false;
        for (DhcpInfo info : vconfig.dhcpInfoMap.values()) {
            if (info.isDefaultL3Network()) {
                success = true;
                Assert.assertEquals(info.getMac(), defaultNic.getMac());
                Assert.assertEquals(info.getIp(), defaultNic.getIp());
                Assert.assertEquals("zstack.org", info.getDnsDomain());
                Assert.assertEquals("vm1.zstack.org", info.getHostname());

                Assert.assertEquals(1, info.getDns().size());
                Assert.assertTrue(String.format("expected:%s, actual:%s", vrPrivateIps, info.getDns()), vrPrivateIps.containsAll(info.getDns()));
            } else {
                Assert.assertEquals(null, info.getHostname());
            }
        }

        Assert.assertTrue(success);
	}
}
