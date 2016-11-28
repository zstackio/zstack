package org.zstack.test.virtualrouter.vyos;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.network.service.virtualrouter.VirtualRouterOfferingInventory;
import org.zstack.network.service.virtualrouter.VirtualRouterSystemTags;
import org.zstack.network.service.virtualrouter.VirtualRouterVmVO;
import org.zstack.simulator.appliancevm.ApplianceVmSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.data.SizeUnit;

import static java.util.Arrays.asList;

/**
 * @author frank
 * @condition use vyos provider
 * 1. specify offering with vyos system tag
 * @test confirm the vr is created from the vyos offering
 */
public class TestVyosPortForwarding1 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    VirtualRouterSimulatorConfig vconfig;
    ApplianceVmSimulatorConfig aconfig;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/virtualRouter/TestVyosPortForwarding1.xml", con);
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("VirtualRouterSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("PortForwarding.xml");
        deployer.addSpringConfig("vip.xml");
        deployer.addSpringConfig("vyos.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        vconfig = loader.getComponent(VirtualRouterSimulatorConfig.class);
        aconfig = loader.getComponent(ApplianceVmSimulatorConfig.class);
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
        L3NetworkInventory publicNw = deployer.l3Networks.get("PublicNetwork");
        ImageInventory image = deployer.images.get("TestImage");
        VirtualRouterOfferingInventory offering = new VirtualRouterOfferingInventory();
        ZoneInventory zone = deployer.zones.get("Zone1");
        offering.setZoneUuid(zone.getUuid());
        offering.setName("vyos");
        offering.setCpuNum(3);
        offering.setMemorySize(SizeUnit.GIGABYTE.toByte(1));
        offering.setManagementNetworkUuid(publicNw.getUuid());
        offering.setPublicNetworkUuid(publicNw.getUuid());
        offering.setImageUuid(image.getUuid());
        offering.setCpuSpeed(1);
        offering.setType(VirtualRouterConstant.VIRTUAL_ROUTER_OFFERING_TYPE);
        api.createVirtualRouterOffering(offering, asList(VirtualRouterSystemTags.VYOS_OFFERING.getTagFormat()), null);

        L3NetworkInventory l3 = deployer.l3Networks.get("GuestNetwork");
        InstanceOfferingInventory ioinv = deployer.instanceOfferings.get("TestInstanceOffering");

        VmCreator creator = new VmCreator(api);
        creator.name = "test";
        creator.instanceOfferingUuid = ioinv.getUuid();
        creator.imageUuid = image.getUuid();
        creator.addL3Network(l3.getUuid());
        creator.timeout = 100000000;
        creator.create();

        VirtualRouterVmVO vr = dbf.listAll(VirtualRouterVmVO.class).get(0);
        Assert.assertEquals(offering.getCpuNum(), vr.getCpuNum());
        Assert.assertEquals(offering.getMemorySize(), vr.getMemorySize());
    }
}
