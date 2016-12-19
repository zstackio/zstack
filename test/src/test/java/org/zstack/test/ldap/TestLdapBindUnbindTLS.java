package org.zstack.test.ldap;

import com.unboundid.ldap.sdk.LDAPException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.query.QueryCondition;
import org.zstack.ldap.*;
import org.zstack.test.Api;
import org.zstack.test.ApiSender;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class TestLdapBindUnbindTLS {
    CLogger logger = Utils.getLogger(TestLdapBindUnbindTLS.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    SessionInventory session;
    LdapManager ldapManager;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();

        deployer = new Deployer("deployerXml/ldap/TestLdap.xml");
        deployer.addSpringConfig("LdapManagerImpl.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        ldapManager = loader.getComponent(LdapManager.class);
        bus = loader.getComponent(CloudBus.class);
        session = api.loginAsAdmin();
    }

    private void queryLdapServer() throws ApiSenderException {
        ApiSender sender = api.getApiSender();

        // query ldap server
        APIQueryLdapServerMsg msg12 = new APIQueryLdapServerMsg();
        msg12.setConditions(new ArrayList<QueryCondition>());
        msg12.setSession(session);
        APIQueryLdapServerReply reply12 = sender.call(msg12, APIQueryLdapServerReply.class);
        logger.debug(reply12.getInventories().stream().map(LdapServerInventory::getUrl).collect(Collectors.joining(", ")));
    }

    @Test
    public void test() throws ApiSenderException, LDAPException {
        ApiSender sender = api.getApiSender();
        String url = "ldap://172.20.12.176:389";
        String basedn = "dc=learnitguide,dc=net";
        String managerName = "cn=Manager,dc=learnitguide,dc=net";
        String managerPassword = "password";
        String uid = "star.guo";
        String password = "miao";
        // add ldap server
        APIAddLdapServerMsg msg13 = new APIAddLdapServerMsg();
        msg13.setName("miao");
        msg13.setDescription("miao desc");
        msg13.setUrl(url);
        msg13.setBase(basedn);
        msg13.setUsername("");
        msg13.setPassword("");
        msg13.setEncryption("TLS");
        msg13.setSession(session);
        APIAddLdapServerEvent evt13 = sender.send(msg13, APIAddLdapServerEvent.class);
        logger.debug(evt13.getInventory().getName());
        queryLdapServer();

        // test conn
        APITestAddLdapServerConnectionMsg msg211 = new APITestAddLdapServerConnectionMsg();
        msg211.setName("miao");
        msg211.setDescription("miao desc");
        msg211.setUrl(url);
        msg211.setBase(basedn);
        msg211.setUsername(managerName);
        msg211.setPassword(managerPassword);
        msg211.setEncryption("TLS");
        msg211.setSession(session);
        msg211.setTimeout(10);
        APITestAddLdapServerConnectionEvent evt211 = sender.send(msg211, APITestAddLdapServerConnectionEvent.class);
        logger.debug(evt211.getInventory().getName());


        // bind account
        AccountInventory ai1 = api.createAccount("ldapuser1", "hello-kitty");
        APICreateLdapBindingMsg msg2 = new APICreateLdapBindingMsg();
        msg2.setAccountUuid(ai1.getUuid());
        msg2.setLdapUid(uid);
        msg2.setSession(session);
        APICreateLdapBindingEvent evt2 = sender.send(msg2, APICreateLdapBindingEvent.class);
        logger.debug(evt2.getInventory().getUuid());


        // login with right password
        APILogInByLdapMsg msg3 = new APILogInByLdapMsg();
        msg3.setUid(uid);
        msg3.setPassword(password);
        msg3.setServiceId(bus.makeLocalServiceId(LdapConstant.SERVICE_ID));
        APILogInByLdapReply reply3 = sender.call(msg3, APILogInByLdapReply.class);
        logger.debug(reply3.getInventory().getAccountUuid());
        logger.debug(reply3.getAccountInventory().getName());

        // login with wrong password
        thrown.expect(ApiSenderException.class);
        //thrown.expectMessage("");

        APILogInByLdapMsg msg31 = new APILogInByLdapMsg();
        msg31.setUid(uid);
        msg31.setPassword("wrong password");
        msg31.setServiceId(bus.makeLocalServiceId(LdapConstant.SERVICE_ID));
        APILogInByLdapReply reply31 = sender.call(msg31, APILogInByLdapReply.class);
        logger.debug(reply31.getInventory().getAccountUuid());
        logger.debug(reply31.getAccountInventory().getName());

        //

    }
}
