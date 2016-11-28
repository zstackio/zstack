package org.zstack.test.network;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.network.service.flat.APIGetL3NetworkDhcpIpAddressMsg;
import org.zstack.network.service.flat.APIGetL3NetworkDhcpIpAddressReply;
import org.zstack.network.service.flat.FlatNetworkServiceConstant;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.vip.VipVO;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.concurrent.TimeUnit;

public class TestGetL3NetworkDhcpIpAddress {
    CLogger logger = Utils.getLogger(TestGetL3NetworkDhcpIpAddress.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/network/TestAcquireIp.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException {
        L3NetworkInventory l3nw = deployer.l3Networks.get("TestL3Network1");
        VipInventory ip = api.acquireIp(l3nw.getUuid());
        VipVO ipvo = dbf.findByUuid(ip.getUuid(), VipVO.class);
        Assert.assertNotNull(ipvo);
        api.releaseIp(ip.getUuid());
        ipvo = dbf.findByUuid(ip.getUuid(), VipVO.class);
        Assert.assertNull(ipvo);
        APIGetL3NetworkDhcpIpAddressMsg msg = new APIGetL3NetworkDhcpIpAddressMsg();
        msg.setL3NetworkUuid(l3nw.getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, FlatNetworkServiceConstant.SERVICE_ID, l3nw.getUuid());
        msg.setTimeout(TimeUnit.SECONDS.toMillis(15));
        APIGetL3NetworkDhcpIpAddressReply reply = (APIGetL3NetworkDhcpIpAddressReply) bus.call(msg);
        logger.info("!!!!!!APIGetL3NetworkDhcpIpAddressMsg!!!!" + reply.getIp());
    }

}
