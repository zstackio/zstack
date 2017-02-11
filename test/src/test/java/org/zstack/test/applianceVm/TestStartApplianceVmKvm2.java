package org.zstack.test.applianceVm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.appliancevm.*;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.kvm.KVMAddons;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.simulator.appliancevm.ApplianceVmSimulatorConfig;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestStartApplianceVmKvm2 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    ApplianceVmFacade apvmf;
    ApplianceVmType type = new ApplianceVmType("TestApplianceVmType");
    CountDownLatch latch = new CountDownLatch(1);
    boolean success = false;
    ApplianceVmSimulatorConfig config;
    KVMSimulatorConfig kconfig;
    ApplianceVmKvmBackend aext;
    ApplianceVmInventory target;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/applianceVm/TestApplianceVmKvm.xml", con);
        deployer.addSpringConfig("ApplianceVmSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        apvmf = loader.getComponent(ApplianceVmFacade.class);
        config = loader.getComponent(ApplianceVmSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        aext = loader.getComponent(ApplianceVmKvmBackend.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        ApplianceVmSpec spec = new ApplianceVmSpec();

        L3NetworkInventory l31 = deployer.l3Networks.get("TestL3Network1");
        L3NetworkInventory l32 = deployer.l3Networks.get("TestL3Network2");
        L3NetworkInventory l33 = deployer.l3Networks.get("TestL3Network3");
        ImageInventory image = deployer.images.get("TestImage");
        InstanceOfferingInventory io = deployer.instanceOfferings.get("TestInstanceOffering");

        final ApplianceVmNicSpec mgmtNic = new ApplianceVmNicSpec();
        mgmtNic.setL3NetworkUuid(l31.getUuid());

        spec.setManagementNic(mgmtNic);
        spec.setName("testApplianceVm");
        spec.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
        spec.setApplianceVmType(type);
        spec.setInstanceOffering(io);
        spec.setTemplate(image);
        spec.setApplianceVmType(type);
        spec.setDefaultRouteL3Network(l32);

        final ApplianceVmNicSpec nic1 = new ApplianceVmNicSpec();
        nic1.setL3NetworkUuid(l32.getUuid());
        nic1.setAcquireOnNetwork(false);
        nic1.setGateway("10.10.2.1");
        nic1.setIp("10.10.2.10");
        nic1.setNetmask("255.0.0.0");
        spec.getAdditionalNics().add(nic1);

        final ApplianceVmNicSpec nic2 = new ApplianceVmNicSpec();
        nic2.setL3NetworkUuid(l33.getUuid());
        spec.getAdditionalNics().add(nic2);

        final CountDownLatch slatch = new CountDownLatch(1);
        apvmf.createApplianceVm(spec, new ReturnValueCompletion<ApplianceVmInventory>(null) {
            @Override
            public void success(ApplianceVmInventory returnValue) {
                target = returnValue;
                slatch.countDown();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                slatch.countDown();
            }
        });

        slatch.await(30, TimeUnit.SECONDS);
        Assert.assertNotNull(target);

        final CountDownLatch stopLatch = new CountDownLatch(1);
        apvmf.stopApplianceVm(target.getUuid(), new ReturnValueCompletion<ApplianceVmInventory>(null) {
            @Override
            public void success(ApplianceVmInventory returnValue) {
                success = true;
                stopLatch.countDown();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                success = false;
                stopLatch.countDown();
            }
        });

        stopLatch.await(30, TimeUnit.SECONDS);
        Assert.assertTrue(success);
        success = false;

        kconfig.startVmCmd = null;
        apvmf.startApplianceVm(target.getUuid(), new ReturnValueCompletion<ApplianceVmInventory>(null) {

            @Override
            public void success(ApplianceVmInventory vm) {
                try {
                    KVMAgentCommands.StartVmCmd cmd = kconfig.startVmCmd;
                    Object ochan = cmd.getAddons().get(KVMAddons.Channel.NAME);
                    KVMAddons.Channel chan = JSONObjectUtil.rehashObject(ochan, KVMAddons.Channel.class);
                    String socketPath = aext.makeChannelSocketPath(vm.getUuid());
                    Assert.assertEquals(socketPath, chan.getSocketPath());
                    Assert.assertEquals("applianceVm.vport", chan.getTargetName());
                    success = true;
                } finally {
                    latch.countDown();
                }
            }

            @Override
            public void fail(ErrorCode errorCode) {
                success = false;
                latch.countDown();
            }
        });

        latch.await(30, TimeUnit.SECONDS);
        Assert.assertTrue(success);
    }
}
