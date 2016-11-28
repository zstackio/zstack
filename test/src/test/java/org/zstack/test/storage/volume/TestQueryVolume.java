package org.zstack.test.storage.volume;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.query.QueryOp;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.APIQueryVolumeMsg;
import org.zstack.header.volume.APIQueryVolumeReply;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.identity.IdentityCreator;
import org.zstack.test.search.QueryTestValidator;
import org.zstack.utils.data.SizeUnit;

import java.util.List;

public class TestQueryVolume {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/volume/TestQueryVolume.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        IdentityCreator identityCreator = new IdentityCreator(api);
        identityCreator.useAccount("test");
        SessionInventory session = identityCreator.getAccountSession();
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        List<VolumeInventory> volumes = vm.getAllVolumes();
        for (VolumeInventory vol : volumes) {
            QueryTestValidator.validateEQ(new APIQueryVolumeMsg(), api, APIQueryVolumeReply.class, vol, session);
            QueryTestValidator.validateRandomEQConjunction(new APIQueryVolumeMsg(), api, APIQueryVolumeReply.class, vol, session, 3);
        }

        APIQueryVolumeMsg msg = new APIQueryVolumeMsg();
        QueryCondition c = new QueryCondition();
        c.setName("size");
        c.setOp(QueryOp.GT_AND_EQ.toString());
        c.setValue(String.valueOf(SizeUnit.GIGABYTE.toByte(100)));
        msg.getConditions().add(c);
        APIQueryVolumeReply reply = api.query(msg, APIQueryVolumeReply.class, session);
        Assert.assertEquals(2, reply.getInventories().size());

        msg = new APIQueryVolumeMsg();
        c = new QueryCondition();
        c.setName("size");
        c.setOp(QueryOp.LT.toString());
        c.setValue(String.valueOf(SizeUnit.GIGABYTE.toByte(120)));
        msg.getConditions().add(c);
        reply = api.query(msg, APIQueryVolumeReply.class, session);
        Assert.assertEquals(2, reply.getInventories().size());
    }
}
