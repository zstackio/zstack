package org.zstack.test.compute.hostallocator;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.apimediator.ApiMediatorConstant;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.*;
import org.zstack.test.Api;
import org.zstack.test.ApiSender;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import java.util.ArrayList;
import java.util.List;

/**
 * 1. have 5 hosts
 * 2. create 100 vm
 * <p>
 * confirm vm are random on each hosts
 */
public class TestDefaultHostAllocationStrategy3 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/hostAllocator/TestHostAllocator3.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    private class VmCreator {
        List<String> l3NetworkUuids = new ArrayList<String>();
        String imageUuid;
        String instanceOfferingUuid;
        List<String> diskOfferingUuids = new ArrayList<String>();
        String zoneUuid;
        String clusterUUid;
        String hostUuid;
        String name = "vm";

        void addL3Network(String uuid) {
            l3NetworkUuids.add(uuid);
        }

        void addDisk(String uuid) {
            diskOfferingUuids.add(uuid);
        }

        VmInstanceInventory create() throws ApiSenderException {
            APICreateVmInstanceMsg msg = new APICreateVmInstanceMsg();
            msg.setClusterUuid(clusterUUid);
            msg.setImageUuid(imageUuid);
            msg.setName(name);
            msg.setHostUuid(hostUuid);
            msg.setDataDiskOfferingUuids(diskOfferingUuids);
            msg.setInstanceOfferingUuid(instanceOfferingUuid);
            msg.setL3NetworkUuids(l3NetworkUuids);
            msg.setType(VmInstanceConstant.USER_VM_TYPE);
            msg.setZoneUuid(zoneUuid);
            msg.setHostUuid(hostUuid);
            msg.setServiceId(ApiMediatorConstant.SERVICE_ID);
            msg.setSession(api.getAdminSession());
            ApiSender sender = new ApiSender();
            APICreateVmInstanceEvent evt = sender.send(msg, APICreateVmInstanceEvent.class);
            return evt.getInventory();
        }
    }

    @Test
    public void test() throws ApiSenderException {
        L3NetworkInventory l3 = deployer.l3Networks.get("l3Network1");
        InstanceOfferingInventory instanceOffering = deployer.instanceOfferings.get("instanceOffering512M512HZ");
        ImageInventory imageInventory = deployer.images.get("image1");

        List<HostVO> hosts = dbf.listAll(HostVO.class);
        List<String> huuids = CollectionUtils.transformToList(hosts, new Function<String, HostVO>() {
            @Override
            public String call(HostVO arg) {
                return arg.getUuid();
            }
        });

        int num = 100;

        for (int i = 0; i < num; i++) {
            VmCreator creator = new VmCreator();
            creator.addL3Network(l3.getUuid());
            creator.imageUuid = imageInventory.getUuid();
            creator.instanceOfferingUuid = instanceOffering.getUuid();
            creator.create();
        }

        for (String huuid : huuids) {
            SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
            q.add(VmInstanceVO_.hostUuid, Op.EQ, huuid);
            long count = q.count();
            Assert.assertTrue(count > 0);
        }
    }
}
