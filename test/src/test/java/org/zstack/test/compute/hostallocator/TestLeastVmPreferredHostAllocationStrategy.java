package org.zstack.test.compute.hostallocator;

import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.apimediator.ApiMediatorConstant;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.host.APIAddHostEvent;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.simulator.APIAddSimulatorHostMsg;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * 1. add 100 hosts
 * 2. start 2000 vms
 * <p>
 * see if VMs are equally distributed
 */
public class TestLeastVmPreferredHostAllocationStrategy {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    int hostNum = 100;
    int vmNum = 2000;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/hostAllocator/TestHostAllocator.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException {
        L3NetworkInventory l3 = deployer.l3Networks.get("l3Network1");
        ImageInventory imageInventory = deployer.images.get("image1");
        ClusterInventory cluster = deployer.clusters.get("cluster1");
        InstanceOfferingInventory ins = deployer.instanceOfferings.get("instanceOffering512M512HZ");
        InstanceOfferingInventory ios = new InstanceOfferingInventory();
        ios.setName("lastVmPreferred");
        ios.setAllocatorStrategy(HostAllocatorConstant.LEAST_VM_PREFERRED_HOST_ALLOCATOR_STRATEGY_TYPE);
        ios.setCpuNum(1);
        ios.setCpuSpeed(1);
        ios.setMemorySize(SizeUnit.MEGABYTE.toByte(512));
        ios = api.addInstanceOffering(ios);

        long sip = NetworkUtils.ipv4StringToLong("192.168.0.1");
        for (int i = 0; i < hostNum; i++) {
            APIAddSimulatorHostMsg amsg = new APIAddSimulatorHostMsg();
            amsg.setCpuCapacity(8 * 2600);
            amsg.setMemoryCapacity(SizeUnit.GIGABYTE.toByte(32));
            amsg.setClusterUuid(cluster.getUuid());
            amsg.setManagementIp(NetworkUtils.longToIpv4String(sip + i));
            amsg.setName(String.format("h%s", i));
            amsg.setServiceId(ApiMediatorConstant.SERVICE_ID);
            amsg.setSession(api.getAdminSession());
            ApiSender sender = new ApiSender();
            sender.send(amsg, APIAddHostEvent.class);
        }

        for (int i = 0; i < vmNum; i++) {
            VmCreator creator = new VmCreator(api);
            creator.addL3Network(l3.getUuid());
            creator.imageUuid = imageInventory.getUuid();
            creator.instanceOfferingUuid = ios.getUuid();
            //creator.instanceOfferingUuid = ins.getUuid();
            VmInstanceInventory vm = creator.create();
        }

        List<Tuple> ts = new Callable<List<Tuple>>() {
            @Override
            @Transactional(readOnly = true)
            public List<Tuple> call() {
                String sql = "select count(vm), host.name from VmInstanceVO vm, HostVO host where vm.hostUuid = host.uuid group by host.uuid";
                TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                return q.getResultList();
            }
        }.call();

        for (Tuple t : ts) {
            long num = t.get(0, Long.class);
            String name = t.get(1, String.class);
            System.out.println(String.format("%s: %s", name, num));
        }
    }
}
