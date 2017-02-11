package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmCreateOnHypervisorFlow;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostInventory;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestVmCreateOnHypervisorFlow {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    CountDownLatch latch = new CountDownLatch(1);
    boolean isSuccess = false;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestVmCreateOnHypervisorFlow.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws InterruptedException, ApiSenderException {
        HostInventory hinv = api.listHosts(null).get(0);

        VmInstanceInventory vminv = new VmInstanceInventory();
        vminv.setUuid(Platform.getUuid());
        VmInstanceSpec spec = new VmInstanceSpec();
        spec.setVmInventory(vminv);
        spec.setDestHost(hinv);
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain().then(new VmCreateOnHypervisorFlow());
        chain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
        chain.done(new FlowDoneHandler(null) {
            @Override
            public void handle(Map data) {
                isSuccess = true;
                latch.countDown();
            }
        }).error(new FlowErrorHandler(null) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                isSuccess = false;
                latch.countDown();
            }
        }).start();

        latch.await(2, TimeUnit.MINUTES);
        Assert.assertTrue(isSuccess);
    }

}
