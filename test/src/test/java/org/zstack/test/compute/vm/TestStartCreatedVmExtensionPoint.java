package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

import java.util.ArrayList;
import java.util.List;

public class TestStartCreatedVmExtensionPoint {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    VmInstanceStartNewCreatedVmExtension ext;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestNewCreatedVmExtensionPoint.xml");
        deployer.addSpringConfig("VmInstanceStartNewCreatedVmExtension.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        ext = loader.getComponent(VmInstanceStartNewCreatedVmExtension.class);
    }

    @Test
    public void test() throws ApiSenderException {
        InstanceOfferingInventory ioinv = api.listInstanceOffering(null).get(0);
        ImageInventory iminv = api.listImage(null).get(0);
        DiskOfferingInventory dinv = api.listDiskOffering(null).get(0);
        L3NetworkInventory ninv = api.listL3Network(null).get(0);
        VmInstanceInventory vm = new VmInstanceInventory();
        vm.setDescription("TestVm");
        vm.setName("TestVm");
        vm.setType(VmInstanceConstant.USER_VM_TYPE);
        vm.setInstanceOfferingUuid(ioinv.getUuid());
        vm.setImageUuid(iminv.getUuid());
        List<String> nws = new ArrayList<String>();
        nws.add(ninv.getUuid());

        ext.setPreventStart(true);
        try {
            api.createVmByFullConfig(vm, dinv.getUuid(), nws, null);
        } catch (ApiSenderException e) {
        }

        ext.setPreventStart(false);
        api.createVmByFullConfig(vm, dinv.getUuid(), nws, null);

        Assert.assertTrue(ext.isBeforeCalled());
        Assert.assertTrue(ext.isAfterCalled());
    }
}
