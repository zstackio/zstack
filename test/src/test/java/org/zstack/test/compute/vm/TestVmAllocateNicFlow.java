package org.zstack.test.compute.vm;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmAllocateNicFlow;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.UsedIpVO;
import org.zstack.header.simulator.SimulatorConstant;
import org.zstack.header.vm.*;
import org.zstack.identity.AccountManager;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestVmAllocateNicFlow {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    CountDownLatch latch = new CountDownLatch(1);
    AccountManager acntMgr;
    boolean isSuccess = false;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestVmAllocateNicFlow.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        acntMgr = loader.getComponent(AccountManager.class);
    }

    @Test
    public void test() throws InterruptedException, ApiSenderException {
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain().then(new VmAllocateNicFlow());

        final List<L3NetworkInventory> l3Networks = api.listL3Network(null);

        InstanceOfferingInventory ioinv = api.listInstanceOffering(null).get(0);
        ImageInventory iminv = api.listImage(null).get(0);
        VmInstanceVO vo = new VmInstanceVO();
        vo.setInstanceOfferingUuid(ioinv.getUuid());
        vo.setState(VmInstanceState.Created);
        vo.setHypervisorType(SimulatorConstant.SIMULATOR_HYPERVISOR_TYPE);
        vo.setType(VmInstanceConstant.USER_VM_TYPE);
        vo.setImageUuid(iminv.getUuid());
        vo.setName("TestVm");
        vo.setPlatform(ImagePlatform.Linux.toString());
        vo.setUuid(Platform.getUuid());
        vo.setInternalId(10);
        dbf.persist(vo);

        acntMgr.createAccountResourceRef(api.getAdminSession().getAccountUuid(), vo.getUuid(), VmInstanceVO.class);

        VmInstanceInventory vminv = VmInstanceInventory.valueOf(vo);
        VmInstanceSpec spec = new VmInstanceSpec();
        spec.setVmInventory(vminv);
        spec.setL3Networks(l3Networks);
        spec.getImageSpec().setInventory(iminv);
        chain.getData().put(VmInstanceConstant.Params.VmInstanceSpec.toString(), spec);
        chain.done(new FlowDoneHandler(null) {
            @Override
            public void handle(Map data) {
                try {
                    VmInstanceSpec ret = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
                    Assert.assertEquals(l3Networks.size(), ret.getDestNics().size());
                    for (VmNicInventory nic : ret.getDestNics()) {
                        UsedIpVO ip = dbf.findByUuid(nic.getUsedIpUuid(), UsedIpVO.class);
                        Assert.assertNotNull(ip);
                        Assert.assertEquals(ip.getIp(), nic.getIp());
                        VmNicVO nvo = dbf.findByUuid(nic.getUuid(), VmNicVO.class);
                        Assert.assertNotNull(nvo);
                        Assert.assertEquals(nvo.getIp(), nic.getIp());
                        Assert.assertEquals(nvo.getMac(), nic.getMac());
                    }
                    isSuccess = true;
                } finally {
                    latch.countDown();
                }
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
