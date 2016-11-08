package org.zstack.test.virtualrouter;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.appliancevm.ApplianceVmConstant;
import org.zstack.appliancevm.ApplianceVmVO;
import org.zstack.appliancevm.ApplianceVmVO_;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.apimediator.ApiMediatorConstant;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.vm.*;
import org.zstack.network.service.virtualrouter.VirtualRouterCommands.DhcpInfo;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestVirtualRouterApplyExistingDhcp {
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
		deployer = new Deployer("deployerXml/virtualRouter/startVirtualRouter.xml", con);
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
	
	private List<String> getDnsOfL3Network(String l3NetworkUuid) {
		L3NetworkVO l3 = dbf.findByUuid(l3NetworkUuid, L3NetworkVO.class);
		return L3NetworkInventory.valueOf(l3).getDns();
	}
	
	@Test
	public void test() throws ApiSenderException, InterruptedException {
		ImageInventory iminv = deployer.images.get("TestImage");
		InstanceOfferingInventory ioinv = deployer.instanceOfferings.get("TestInstanceOffering");
		L3NetworkInventory l3inv = deployer.l3Networks.get("TestL3Network2");
		APICreateVmInstanceMsg msg = new APICreateVmInstanceMsg();
		msg.setImageUuid(iminv.getUuid());
		msg.setInstanceOfferingUuid(ioinv.getUuid());
		List<String> l3uuids = new ArrayList<String>();
		l3uuids.add(l3inv.getUuid());
		msg.setL3NetworkUuids(l3uuids);
		msg.setName("TestVm");
		msg.setSession(session);
		msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
		msg.setType(VmInstanceConstant.USER_VM_TYPE);
		ApiSender sender = api.getApiSender();
		APICreateVmInstanceEvent evt = sender.send(msg, APICreateVmInstanceEvent.class);
		// start total 3 vms
		sender.send(msg, APICreateVmInstanceEvent.class);
		sender.send(msg, APICreateVmInstanceEvent.class);
		
		SimpleQuery<ApplianceVmVO> q = dbf.createQuery(ApplianceVmVO.class);
		q.add(ApplianceVmVO_.type, Op.EQ, ApplianceVmConstant.APPLIANCE_VM_TYPE);
		q.add(VmInstanceVO_.state, Op.EQ, VmInstanceState.Running);
		VmInstanceVO vr = q.find();
		api.stopVmInstance(vr.getUuid());
		api.startVmInstance(vr.getUuid());

		SimpleQuery<VmInstanceVO> uq = dbf.createQuery(VmInstanceVO.class);
		uq.add(VmInstanceVO_.type, Op.EQ, VmInstanceConstant.USER_VM_TYPE);
		List<VmInstanceVO> userVms = uq.list();
		List<VmNicVO> nics = new ArrayList<VmNicVO>();
		for (VmInstanceVO vm : userVms) {
			nics.addAll(vm.getVmNics());
		}
		
		Map<String, DhcpInfo> infos = new HashMap<String, DhcpInfo>();
		for (DhcpInfo info : vconfig.dhcpInfos) {
			infos.put(info.getMac(), info);
		}
		
		for (VmNicVO nic : nics) {
			DhcpInfo info = infos.get(nic.getMac());
			Assert.assertNotNull(info);
			Assert.assertEquals(nic.getIp(), info.getIp());
			Assert.assertEquals(nic.getNetmask(), info.getNetmask());
			Assert.assertEquals(nic.getGateway(), info.getGateway());
			Assert.assertTrue(info.getDns().containsAll(getDnsOfL3Network(nic.getL3NetworkUuid())));
		}
	}
}
