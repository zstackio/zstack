package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.*;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.APIQueryImageMsg;
import org.zstack.header.image.APIQueryImageReply;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.APIQueryL3NetworkMsg;
import org.zstack.header.network.l3.APIQueryL3NetworkReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.query.QueryCondition;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.identity.IdentityCreator;

import java.util.ArrayList;

import static org.zstack.utils.CollectionDSL.list;

/**
 * 1. create resources with account1
 * <p>
 * confirm account 2 cannot see resources of the account 1
 * <p>
 * 2. share the resources to hte account 2
 * <p>
 * confirm account 2 can see resources of the account 1
 */
public class TestQueryVm6 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestQueryVm.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException {
        InstanceOfferingInventory iniov = deployer.instanceOfferings.get("TestInstanceOffering");
        ImageInventory img = deployer.images.get("TestImage");
        DiskOfferingInventory dov = deployer.diskOfferings.get("TestRootDiskOffering");
        L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network1");

        IdentityCreator identityCreator = new IdentityCreator(api);
        AccountInventory a = identityCreator.createAccount("test1", "password");
        SessionInventory session = identityCreator.getAccountSession();

        APIQueryInstanceOfferingMsg qimsg = new APIQueryInstanceOfferingMsg();
        qimsg.setConditions(new ArrayList<QueryCondition>());
        APIQueryInstanceOfferingReply qir = api.query(qimsg, APIQueryInstanceOfferingReply.class, session);
        Assert.assertEquals(0, qir.getInventories().size());

        APIQueryDiskOfferingMsg qdmsg = new APIQueryDiskOfferingMsg();
        qdmsg.setConditions(new ArrayList<QueryCondition>());
        APIQueryDiskOfferingReply qdr = api.query(qdmsg, APIQueryDiskOfferingReply.class, session);
        Assert.assertEquals(0, qdr.getInventories().size());

        APIQueryImageMsg imgmsg = new APIQueryImageMsg();
        imgmsg.setConditions(new ArrayList<QueryCondition>());
        APIQueryImageReply imgr = api.query(imgmsg, APIQueryImageReply.class, session);
        Assert.assertEquals(0, imgr.getInventories().size());

        APIQueryL3NetworkMsg l3msg = new APIQueryL3NetworkMsg();
        l3msg.setConditions(new ArrayList<QueryCondition>());
        APIQueryL3NetworkReply l3r = api.query(l3msg, APIQueryL3NetworkReply.class, session);
        Assert.assertEquals(0, l3r.getInventories().size());

        SessionInventory sessionForAccount1 = api.loginByAccount("test", "password");
        api.shareResource(list(iniov.getUuid(), img.getUuid(), dov.getUuid(), l3.getUuid()), list(a.getUuid()), false, sessionForAccount1);

        qir = api.query(qimsg, APIQueryInstanceOfferingReply.class, session);
        Assert.assertEquals(1, qir.getInventories().size());

        qdr = api.query(qdmsg, APIQueryDiskOfferingReply.class, session);
        Assert.assertEquals(1, qdr.getInventories().size());

        imgr = api.query(imgmsg, APIQueryImageReply.class, session);
        Assert.assertEquals(1, imgr.getInventories().size());

        l3r = api.query(l3msg, APIQueryL3NetworkReply.class, session);
        Assert.assertEquals(1, l3r.getInventories().size());

        IdentityCreator identityCreator1 = new IdentityCreator(api);
        identityCreator1.createAccount("test2", "password");
        SessionInventory sessionForAccount2 = identityCreator1.getAccountSession();

        qir = api.query(qimsg, APIQueryInstanceOfferingReply.class, sessionForAccount2);
        Assert.assertEquals(0, qir.getInventories().size());

        qdr = api.query(qdmsg, APIQueryDiskOfferingReply.class, sessionForAccount2);
        Assert.assertEquals(0, qdr.getInventories().size());

        imgr = api.query(imgmsg, APIQueryImageReply.class, sessionForAccount2);
        Assert.assertEquals(0, imgr.getInventories().size());

        l3r = api.query(l3msg, APIQueryL3NetworkReply.class, sessionForAccount2);
        Assert.assertEquals(0, l3r.getInventories().size());

        api.revokeAllResourceSharing(list(iniov.getUuid(), img.getUuid(), dov.getUuid(), l3.getUuid()), session);
        api.shareResource(list(iniov.getUuid(), img.getUuid(), dov.getUuid(), l3.getUuid()), null, true, sessionForAccount1);

        qir = api.query(qimsg, APIQueryInstanceOfferingReply.class, session);
        Assert.assertEquals(1, qir.getInventories().size());

        qdr = api.query(qdmsg, APIQueryDiskOfferingReply.class, session);
        Assert.assertEquals(1, qdr.getInventories().size());

        imgr = api.query(imgmsg, APIQueryImageReply.class, session);
        Assert.assertEquals(1, imgr.getInventories().size());

        l3r = api.query(l3msg, APIQueryL3NetworkReply.class, session);
        Assert.assertEquals(1, l3r.getInventories().size());

        qir = api.query(qimsg, APIQueryInstanceOfferingReply.class, sessionForAccount2);
        Assert.assertEquals(1, qir.getInventories().size());

        qdr = api.query(qdmsg, APIQueryDiskOfferingReply.class, sessionForAccount2);
        Assert.assertEquals(1, qdr.getInventories().size());

        imgr = api.query(imgmsg, APIQueryImageReply.class, sessionForAccount2);
        Assert.assertEquals(1, imgr.getInventories().size());

        l3r = api.query(l3msg, APIQueryL3NetworkReply.class, sessionForAccount2);
        Assert.assertEquals(1, l3r.getInventories().size());
    }
}
