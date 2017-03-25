package org.zstack.hybrid;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.datacenter.*;
import org.zstack.header.identity.SessionInventory;
import org.zstack.hybrid.core.HybridType;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;

/**
 * Created by mingjian.deng on 17/2/8.
 */
public class TestCreateDataCenter {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    protected static final CLogger logger = Utils.getLogger(TestCreateDataCenter.class);

    private SessionInventory adminSession;
    private ApiSender sender;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestCreateVm.xml");
        deployer.addSpringConfig("mevocoRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        adminSession = api.loginAsAdmin();
        sender = new ApiSender();
        sender.setTimeout(15);
    }

    @Test
    public void test() throws ApiSenderException {
        APIAddDataCenterFromRemoteMsg msg = new APIAddDataCenterFromRemoteMsg();
        msg.setRegionId("cn-hangzhou");
        msg.setType(HybridType.aliyun.toString());
        APIAddDataCenterFromRemoteEvent event = createDataCenter(msg);
        Assert.assertTrue(event.isSuccess());
        Assert.assertNotNull(dbf.findByUuid(event.getInventory().getUuid(), DataCenterVO.class));

        APIQueryDataCenterFromLocalMsg msg2 = new APIQueryDataCenterFromLocalMsg();
        msg2.setSession(adminSession);
        msg2.setConditions(new ArrayList<>());
        APIQueryDataCenterFromLocalReply reply = sender.call(msg2, APIQueryDataCenterFromLocalReply.class);
        Assert.assertEquals(reply.getInventories().get(0).getUuid(), event.getInventory().getUuid());

        APIDeleteDataCenterInLocalMsg msg1 = new APIDeleteDataCenterInLocalMsg();
        msg1.setUuid(event.getInventory().getUuid());
        APIDeleteDataCenterInLocalEvent event1 = deleteDataCenter(msg1);
        Assert.assertTrue(event1.isSuccess());
        Assert.assertNull(dbf.findByUuid(event.getInventory().getUuid(), DataCenterVO.class));

    }

    private APIDeleteDataCenterInLocalEvent deleteDataCenter(APIDeleteDataCenterInLocalMsg msg) throws ApiSenderException {
        msg.setSession(adminSession);
        sender = new ApiSender();
        sender.setTimeout(15);
        APIDeleteDataCenterInLocalEvent event = sender.send(msg, APIDeleteDataCenterInLocalEvent.class);
        return event;
    }

    private APIAddDataCenterFromRemoteEvent createDataCenter(APIAddDataCenterFromRemoteMsg msg) throws ApiSenderException {
        msg.setSession(adminSession);
        APIAddDataCenterFromRemoteEvent event = sender.send(msg, APIAddDataCenterFromRemoteEvent.class);
        return event;
    }
}
