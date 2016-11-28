package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.allocator.HostCapacityVO;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.UsedIpVO;
import org.zstack.header.vm.*;
import org.zstack.header.volume.VolumeVO;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.aop.CloudBusAopProxy;
import org.zstack.test.deployer.Deployer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TestCreateVmFailure {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    CloudBusAopProxy busProxy;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestCreateVmFailure.xml");
        deployer.addSpringConfig("CloudBusAopProxy.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        busProxy = loader.getComponent(CloudBusAopProxy.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        InstanceOfferingInventory ioinv = api.listInstanceOffering(null).get(0);
        ImageInventory iminv = api.listImage(null).get(0);
        List<DiskOfferingInventory> dinvs = api.listDiskOffering(null);
        List<L3NetworkInventory> nwinvs = api.listL3Network(null);
        VmInstanceInventory vm = new VmInstanceInventory();
        vm.setDescription("TestVm");
        vm.setName("TestVm");
        vm.setType(VmInstanceConstant.USER_VM_TYPE);
        vm.setInstanceOfferingUuid(ioinv.getUuid());
        vm.setImageUuid(iminv.getUuid());
        List<String> nws = new ArrayList<String>(nwinvs.size());
        for (L3NetworkInventory nwinv : nwinvs) {
            nws.add(nwinv.getUuid());
        }
        List<String> disks = new ArrayList<String>(1);
        disks.add(dinvs.get(1).getUuid());

        busProxy.addMessage(CreateVmOnHypervisorMsg.class, CloudBusAopProxy.Behavior.FAIL);
        try {
            api.createVmByFullConfig(vm, dinvs.get(0).getUuid(), nws, disks);
        } catch (ApiSenderException e) {
        }

        // wait for async thread complete
        TimeUnit.SECONDS.sleep(5);

        SimpleQuery<VmInstanceVO> vmq = dbf.createQuery(VmInstanceVO.class);
        VmInstanceVO vmvo = vmq.find();
        Assert.assertNull(vmvo);

        SimpleQuery<VolumeVO> volq = dbf.createQuery(VolumeVO.class);
        long count = volq.count();
        Assert.assertEquals(0, count);

        SimpleQuery<VmNicVO> nicq = dbf.createQuery(VmNicVO.class);
        count = nicq.count();
        Assert.assertEquals(0, count);

        SimpleQuery<UsedIpVO> ipq = dbf.createQuery(UsedIpVO.class);
        count = ipq.count();
        Assert.assertEquals(0, count);

        List<HostCapacityVO> hcs = dbf.listAll(HostCapacityVO.class);
        for (HostCapacityVO hc : hcs) {
            Assert.assertEquals(hc.getTotalCpu(), hc.getAvailableCpu());
            Assert.assertEquals(hc.getTotalMemory(), hc.getAvailableMemory());
        }
    }
}
