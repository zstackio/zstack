package org.zstack.test.storage.volume;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.search.APISearchMessage.NOVTriple;
import org.zstack.header.search.SearchOp;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.APIGetVolumeMsg;
import org.zstack.header.volume.APISearchVolumeMsg;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TestSearchVolume2 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/volume/TestSearchVolume2.xml");
        deployer.addSpringConfig("SearchManager.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        DiskOfferingInventory dinv = deployer.diskOfferings.get("TestRootDiskOffering");
        api.createDataVolume("testData", dinv.getUuid());

        TimeUnit.SECONDS.sleep(1);
        APISearchVolumeMsg msg = new APISearchVolumeMsg();
        NOVTriple tl = new NOVTriple();
        tl.setName("state");
        tl.setOp(SearchOp.AND_EQ.toString());
        tl.setVal(VolumeStatus.NotInstantiated.toString());
        msg.getNameOpValueTriples().add(tl);

        String res = api.search(msg);
        List<VolumeInventory> invs = JSONObjectUtil.toCollection(res, ArrayList.class, VolumeInventory.class);
        Assert.assertEquals(1, invs.size());

        VolumeInventory inv0 = invs.get(0);
        APIGetVolumeMsg gmsg = new APIGetVolumeMsg();
        gmsg.setUuid(inv0.getUuid());
        res = api.getInventory(gmsg);
        VolumeInventory vinv = JSONObjectUtil.toObject(res, VolumeInventory.class);
        Assert.assertEquals(inv0.getName(), vinv.getName());

        VmInstanceInventory vm = deployer.vms.get("TestVm");
        api.attachVolumeToVm(vm.getUuid(), inv0.getUuid());
        TimeUnit.MILLISECONDS.sleep(500);
        res = api.getInventory(gmsg);
        vinv = JSONObjectUtil.toObject(res, VolumeInventory.class);
        Assert.assertTrue(vinv.isAttached());
    }
}
