package org.zstack.test.network;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.AccountConstant.StatementEffect;
import org.zstack.header.identity.IdentityErrors;
import org.zstack.header.identity.PolicyInventory.Statement;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.network.l2.APIQueryL2NetworkMsg;
import org.zstack.header.network.l2.APIQueryL2NetworkReply;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l3.*;
import org.zstack.header.network.service.*;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.query.QueryOp;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.identity.IdentityCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;

import static org.zstack.utils.CollectionDSL.list;

public class TestPolicyForL3Network {
    CLogger logger = Utils.getLogger(TestPolicyForL3Network.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/network/TestPolicyForL3Network.xml");
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    IpRangeInventory addIpRange(String l3Uuid, SessionInventory session) throws ApiSenderException {
        return api.addIpRange(l3Uuid, "192.168.0.10", "192.168.0.20", "192.168.0.1", "255.255.255.0", session);
    }

    IpRangeInventory addIpRange(String l3Uuid, String cidr, SessionInventory session) throws ApiSenderException {
        return api.addIpRangeByCidr(l3Uuid, cidr, session);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        L2NetworkInventory l2 = deployer.l2Networks.get("TestL2Network");

        IdentityCreator identityCreator = new IdentityCreator(api);
        identityCreator.useAccount("test");
        identityCreator.createUser("user1", "password");

        Statement s = new Statement();
        s.setName("allow");
        s.setEffect(StatementEffect.Allow);
        s.addAction(String.format("%s:%s", L3NetworkConstant.ACTION_CATEGORY, APICreateL3NetworkMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", L3NetworkConstant.ACTION_CATEGORY, APIChangeL3NetworkStateMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", L3NetworkConstant.ACTION_CATEGORY, APIUpdateL3NetworkMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", L3NetworkConstant.ACTION_CATEGORY, APIDeleteL3NetworkMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", L3NetworkConstant.ACTION_CATEGORY, APIAddIpRangeByNetworkCidrMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", L3NetworkConstant.ACTION_CATEGORY, APIAddIpRangeMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", L3NetworkConstant.ACTION_CATEGORY, APIAttachNetworkServiceToL3NetworkMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", L3NetworkConstant.ACTION_CATEGORY, APIAddDnsToL3NetworkMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", L3NetworkConstant.ACTION_CATEGORY, APIRemoveDnsFromL3NetworkMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", L3NetworkConstant.ACTION_CATEGORY, APIDeleteIpRangeMsg.class.getSimpleName()));
        identityCreator.createPolicy("allow", s);

        identityCreator.attachPolicyToUser("user1", "allow");
        SessionInventory session = identityCreator.userLogin("user1", "password");

        L3NetworkInventory l3 = api.createL3BasicNetwork(l2.getUuid(), session);
        api.updateL3Network(l3, session);
        IpRangeInventory ipr1 = addIpRange(l3.getUuid(), session);
        //IpRangeInventory ipr2 = addIpRange(l3.getUuid(), "10.0.0.0/24", session);

        APIQueryNetworkServiceProviderMsg msg = new APIQueryNetworkServiceProviderMsg();
        msg.addQueryCondition("name", QueryOp.EQ, "VirtualRouter");
        APIQueryNetworkServiceProviderReply reply = api.query(msg, APIQueryNetworkServiceProviderReply.class, session);
        NetworkServiceProviderInventory pinv = reply.getInventories().get(0);
        api.attachNetworkServiceToL3Network(l3.getUuid(), pinv.getUuid(), list("DHCP", "DNS"), session);
        api.addDns(l3.getUuid(), "8.8.8.8", session);
        api.removeDnsFromL3Network("8.8.8.8", l3.getUuid(), session);
        api.deleteIpRange(ipr1.getUuid(), session);
        api.changeL3NetworkState(l3.getUuid(), L3NetworkStateEvent.disable, session);
        api.deleteL3Network(l3.getUuid(), session);

        s = new Statement();
        s.setName("deny");
        s.setEffect(StatementEffect.Deny);
        s.addAction(String.format("%s:%s", L3NetworkConstant.ACTION_CATEGORY, APICreateL3NetworkMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", L3NetworkConstant.ACTION_CATEGORY, APIChangeL3NetworkStateMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", L3NetworkConstant.ACTION_CATEGORY, APIUpdateL3NetworkMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", L3NetworkConstant.ACTION_CATEGORY, APIDeleteL3NetworkMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", L3NetworkConstant.ACTION_CATEGORY, APIAddIpRangeByNetworkCidrMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", L3NetworkConstant.ACTION_CATEGORY, APIAddIpRangeMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", L3NetworkConstant.ACTION_CATEGORY, APIAttachNetworkServiceToL3NetworkMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", L3NetworkConstant.ACTION_CATEGORY, APIAddDnsToL3NetworkMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", L3NetworkConstant.ACTION_CATEGORY, APIRemoveDnsFromL3NetworkMsg.class.getSimpleName()));
        s.addAction(String.format("%s:%s", L3NetworkConstant.ACTION_CATEGORY, APIDeleteIpRangeMsg.class.getSimpleName()));
        identityCreator.createPolicy("deny", s);

        l3 = api.createL3BasicNetwork(l2.getUuid(), session);
        ipr1 = addIpRange(l3.getUuid(), session);

        identityCreator.detachPolicyFromUser("user1", "allow");
        identityCreator.attachPolicyToUser("user1", "deny");

        boolean success = false;
        try {
            api.createL3BasicNetwork(l2.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.updateL3Network(l3, session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            addIpRange(l3.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.attachNetworkServiceToL3Network(l3.getUuid(), pinv.getUuid(), list("DHCP", "DNS"), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.addDns(l3.getUuid(), "8.8.8.8", session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.removeDnsFromL3Network("8.8.8.8", l3.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.deleteIpRange(ipr1.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.changeL3NetworkState(l3.getUuid(), L3NetworkStateEvent.disable, session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.deleteL3Network(l3.getUuid(), session);
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

        l3 = api.createL3BasicNetwork(l2.getUuid(), session);
        api.updateL3Network(l3, session);
        ipr1 = addIpRange(l3.getUuid(), session);
        //ipr2 = addIpRange(l3.getUuid(), "10.0.0.0/24", session);

        msg = new APIQueryNetworkServiceProviderMsg();
        msg.addQueryCondition("name", QueryOp.EQ, "VirtualRouter");
        reply = api.query(msg, APIQueryNetworkServiceProviderReply.class, session);
        pinv = reply.getInventories().get(0);
        api.attachNetworkServiceToL3Network(l3.getUuid(), pinv.getUuid(), list("DHCP", "DNS"), session);
        api.addDns(l3.getUuid(), "8.8.8.8", session);
        api.removeDnsFromL3Network("8.8.8.8", l3.getUuid(), session);
        api.deleteIpRange(ipr1.getUuid(), session);
        api.changeL3NetworkState(l3.getUuid(), L3NetworkStateEvent.disable, session);
        api.deleteL3Network(l3.getUuid(), session);

        l3 = api.createL3BasicNetwork(l2.getUuid(), session);
        ipr1 = addIpRange(l3.getUuid(), session);

        identityCreator.detachPolicyFromGroup("group", "allow");
        identityCreator.attachPolicyToGroup("group", "deny");

        success = false;
        try {
            api.createL3BasicNetwork(l2.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.updateL3Network(l3, session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            addIpRange(l3.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.attachNetworkServiceToL3Network(l3.getUuid(), pinv.getUuid(), list("DHCP", "DNS"), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.addDns(l3.getUuid(), "8.8.8.8", session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.removeDnsFromL3Network("8.8.8.8", l3.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.deleteIpRange(ipr1.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.changeL3NetworkState(l3.getUuid(), L3NetworkStateEvent.disable, session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        success = false;
        try {
            api.deleteL3Network(l3.getUuid(), session);
        } catch (ApiSenderException e) {
            if (IdentityErrors.PERMISSION_DENIED.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        APIQueryL3NetworkMsg ql3 = new APIQueryL3NetworkMsg();
        ql3.setConditions(new ArrayList<QueryCondition>());
        api.query(ql3, APIQueryL3NetworkReply.class, session);

        APIQueryL2NetworkMsg ql2 = new APIQueryL2NetworkMsg();
        ql2.setConditions(new ArrayList<QueryCondition>());
        api.query(ql2, APIQueryL2NetworkReply.class, session);

        APIQueryNetworkServiceL3NetworkRefMsg qref = new APIQueryNetworkServiceL3NetworkRefMsg();
        qref.setConditions(new ArrayList<QueryCondition>());
        api.query(qref, APIQueryNetworkServiceL3NetworkRefReply.class, session);

        APIQueryIpRangeMsg qipr = new APIQueryIpRangeMsg();
        qipr.setConditions(new ArrayList<QueryCondition>());
        api.query(qipr, APIQueryIpRangeReply.class, session);

        api.getFreeIp(l3.getUuid(), null, 100, null, session);
        api.getIpAddressCapacityByAll(session);
        api.getL3NetworkTypes(session);
    }
}
