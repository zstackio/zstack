package org.zstack.test.compute.zone;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.rest.RESTApiDecoder;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.rest.RESTConstant;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.rest.RestAPIResponse;
import org.zstack.header.zone.APICreateZoneEvent;
import org.zstack.header.zone.APICreateZoneMsg;
import org.zstack.simulator.SyncRESTCaller;
import org.zstack.test.Api;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

public class TestCreateZoneByHttpApi {
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;
    RESTFacade restf;
    SessionInventory session;
    CLogger logger = Utils.getLogger(TestCreateZoneByHttpApi.class);

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        con.setPort(8080);
        /* This loads spring application context */
        loader = con.addXml("PortalForUnitTest.xml").addXml("ZoneManager.xml").addXml("AccountManager.xml").addXml("RESTFacade.xml").build();
        dbf = loader.getComponent(DatabaseFacade.class);
        restf = loader.getComponent(RESTFacade.class);
        api = new Api();
        api.startServer();
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws InterruptedException {
        APICreateZoneMsg msg = new APICreateZoneMsg();
        msg.setSession(session);
        msg.setName("TestZone");
        msg.setDescription("TestZone");
        SyncRESTCaller caller = new SyncRESTCaller();
        RestAPIResponse rsp = caller.syncPost(RESTConstant.REST_API_CALL, msg);
        logger.debug(rsp.getResult());
        APICreateZoneEvent evt = (APICreateZoneEvent) RESTApiDecoder.loads(rsp.getResult());
        Assert.assertTrue(evt.isSuccess());
    }
}
