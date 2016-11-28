package org.zstack.test.virtualrouter;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.APICreateInstanceOfferingEvent;
import org.zstack.header.configuration.APIDeleteInstanceOfferingMsg;
import org.zstack.header.configuration.ConfigurationConstant;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.identity.AccountConstant.StatementEffect;
import org.zstack.header.identity.IdentityErrors;
import org.zstack.header.identity.PolicyInventory.Statement;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.network.service.virtualrouter.APICreateVirtualRouterOfferingMsg;
import org.zstack.network.service.virtualrouter.APIQueryVirtualRouterOfferingMsg;
import org.zstack.network.service.virtualrouter.APIQueryVirtualRouterOfferingReply;
import org.zstack.network.service.virtualrouter.VirtualRouterConstant;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.identity.IdentityCreator;
import org.zstack.utils.data.SizeUnit;

import java.util.ArrayList;

public class TestPolicyForVirtualRouterOffering {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/virtualRouter/TestQueryVirtualRouterOffering.xml", con);
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    InstanceOfferingInventory createOffering(String zoneUuid, String l3Uuid, String imgUuid, SessionInventory session) throws ApiSenderException {
        APICreateVirtualRouterOfferingMsg msg = new APICreateVirtualRouterOfferingMsg();
        msg.setName("vr");
        msg.setImageUuid(imgUuid);
        msg.setManagementNetworkUuid(l3Uuid);
        msg.setPublicNetworkUuid(l3Uuid);
        msg.setZoneUuid(zoneUuid);
        msg.setCpuNum(1);
        msg.setCpuSpeed(1);
        msg.setMemorySize(SizeUnit.GIGABYTE.toByte(1));
        msg.setSession(session);
        ApiSender sender = api.getApiSender();
        APICreateInstanceOfferingEvent evt = sender.send(msg, APICreateInstanceOfferingEvent.class);
        return evt.getInventory();
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        ZoneInventory zone = deployer.zones.get("Zone1");
        L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network1");
        ImageInventory img = deployer.images.get("TestImage");

        IdentityCreator identityCreator = new IdentityCreator(api);
        identityCreator.useAccount("test");
        identityCreator.createUser("user1", "password");
        Statement s = new Statement();
        s.setName("allow");
        s.setEffect(StatementEffect.Allow);
        s.addAction(String.format("%s:%s", VirtualRouterConstant.ACTION_CATEGORY, APICreateVirtualRouterOfferingMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", ConfigurationConstant.ACTION_CATEGORY, APIDeleteInstanceOfferingMsg.class.getSimpleName()));
        identityCreator.createPolicy("allow", s);
        identityCreator.attachPolicyToUser("user1", "allow");
        SessionInventory session = identityCreator.userLogin("user1", "password");

        InstanceOfferingInventory offering = createOffering(zone.getUuid(), l3.getUuid(), img.getUuid(), session);
        api.deleteInstanceOffering(offering.getUuid(), session);

        s = new Statement();
        s.setName("deny");
        s.setEffect(StatementEffect.Deny);
        s.addAction(String.format("%s:%s", VirtualRouterConstant.ACTION_CATEGORY, APICreateVirtualRouterOfferingMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", ConfigurationConstant.ACTION_CATEGORY, APIDeleteInstanceOfferingMsg.class.getSimpleName()));
        identityCreator.createPolicy("deny", s);

        offering = createOffering(zone.getUuid(), l3.getUuid(), img.getUuid(), session);

        identityCreator.detachPolicyFromUser("user1", "allow");
        identityCreator.attachPolicyToUser("user1", "deny");

        boolean success = false;
        try {
            createOffering(zone.getUuid(), l3.getUuid(), img.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.deleteInstanceOffering(offering.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        // user and group
        identityCreator.createUser("user2", "password");
        identityCreator.createGroup("group");
        identityCreator.addUserToGroup("user2", "group");
        identityCreator.attachPolicyToGroup("group", "allow");
        session = identityCreator.userLogin("user2", "password");

        offering = createOffering(zone.getUuid(), l3.getUuid(), img.getUuid(), session);
        api.deleteInstanceOffering(offering.getUuid(), session);

        offering = createOffering(zone.getUuid(), l3.getUuid(), img.getUuid(), session);

        identityCreator.detachPolicyFromGroup("group", "allow");
        identityCreator.attachPolicyToGroup("group", "deny");

        success = false;
        try {
            createOffering(zone.getUuid(), l3.getUuid(), img.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.deleteInstanceOffering(offering.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        APIQueryVirtualRouterOfferingMsg qmsg = new APIQueryVirtualRouterOfferingMsg();
        qmsg.setConditions(new ArrayList<QueryCondition>());
        api.query(qmsg, APIQueryVirtualRouterOfferingReply.class, session);
    }
}
