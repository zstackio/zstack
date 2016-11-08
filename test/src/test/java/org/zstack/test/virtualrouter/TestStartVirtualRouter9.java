package org.zstack.test.virtualrouter;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.appliancevm.ApplianceVmVO;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.apimediator.ApiMediatorConstant;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.APICreateVmInstanceEvent;
import org.zstack.header.vm.APICreateVmInstanceMsg;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.network.service.virtualrouter.VirtualRouterSystemTags;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public class TestStartVirtualRouter9 {
	CLogger logger = Utils.getLogger(TestStartVirtualRouter9.class);
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

	@Test
	public void test() throws ApiSenderException {
		ImageInventory iminv = deployer.images.get("TestImage");
		InstanceOfferingInventory ioinv = deployer.instanceOfferings.get("TestInstanceOffering");
		L3NetworkInventory l3inv = deployer.l3Networks.get("TestL3Network2");
        InstanceOfferingInventory vroffering1 = deployer.instanceOfferings.get("virtualRouterOffering1");
        VirtualRouterSystemTags.VR_OFFERING_GUEST_NETWORK.createTag(vroffering1.getUuid(),
                map(e(VirtualRouterSystemTags.VR_OFFERING_GUEST_NETWORK_TOKEN, l3inv.getUuid())));

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
		sender.send(msg, APICreateVmInstanceEvent.class);

        ApplianceVmVO apvm = dbf.listAll(ApplianceVmVO.class).get(0);
        Assert.assertEquals(vroffering1.getUuid(), apvm.getInstanceOfferingUuid());
    }

}
