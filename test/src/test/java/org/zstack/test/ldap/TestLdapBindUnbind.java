package org.zstack.test.ldap;

import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPInterface;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchScope;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.zapodot.junit.ldap.EmbeddedLdapRule;
import org.zapodot.junit.ldap.EmbeddedLdapRuleBuilder;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.header.identity.APILogInReply;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.identity.login.APILogInMsg;
import org.zstack.header.query.QueryCondition;
import org.zstack.ldap.*;
import org.zstack.ldap.api.APIAddLdapServerEvent;
import org.zstack.ldap.api.APIAddLdapServerMsg;
import org.zstack.ldap.api.APICreateLdapBindingEvent;
import org.zstack.ldap.api.APICreateLdapBindingMsg;
import org.zstack.ldap.api.APIDeleteLdapBindingEvent;
import org.zstack.ldap.api.APIDeleteLdapBindingMsg;
import org.zstack.ldap.api.APIQueryLdapServerMsg;
import org.zstack.ldap.api.APIQueryLdapServerReply;
import org.zstack.ldap.entity.LdapServerInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSender;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class TestLdapBindUnbind {
    CLogger logger = Utils.getLogger(TestLdapBindUnbind.class);

    public static final String DOMAIN_DSN = "dc=example,dc=com";
    @Rule
    public EmbeddedLdapRule embeddedLdapRule = EmbeddedLdapRuleBuilder.newInstance().bindingToPort(1888).
            usingDomainDsn(DOMAIN_DSN).importingLdifs("users-import.ldif").build();

    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    SessionInventory session;
    LdapManager ldapManager;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();

        deployer = new Deployer("deployerXml/ldap/TestLdap.xml");
        deployer.addSpringConfig("accountImport.xml");
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
        final LDAPInterface ldapConnection = embeddedLdapRule.ldapConnection();
        final SearchResult searchResult = ldapConnection.search(DOMAIN_DSN, SearchScope.SUB, "(objectClass=person)");
        Assert.assertEquals(3, searchResult.getEntryCount());

        ApiSender sender = api.getApiSender();

        // add ldap server
        APIAddLdapServerMsg msg13 = new APIAddLdapServerMsg();
        msg13.setName("miao");
        msg13.setDescription("miao desc");
        msg13.setUrl("ldap://localhost:1888");
        msg13.setBase(DOMAIN_DSN);
        msg13.setUsername("");
        msg13.setPassword("");
        msg13.setEncryption("None");
        msg13.setSession(session);
        APIAddLdapServerEvent evt13 = sender.send(msg13, APIAddLdapServerEvent.class);
        logger.debug(evt13.getInventory().getName());
        queryLdapServer();

        // bind account with a not exist uid
        try {
            AccountInventory ai12 = api.createAccount("ldapuser3", "hello-kitty");
            APICreateLdapBindingMsg msg22 = new APICreateLdapBindingMsg();
            msg22.setAccountUuid(ai12.getUuid());
            msg22.setLdapUid("Not exist");
            msg22.setSession(session);
            APICreateLdapBindingEvent evt22 = sender.send(msg22, APICreateLdapBindingEvent.class);
            logger.debug("" + evt22.getInventory().getId());
        } catch (Exception e) {
            logger.trace("bind account with a non-existent uid", e);
        }

        // bind a not exist account with a not exist uid
        try {
            APICreateLdapBindingMsg msg22 = new APICreateLdapBindingMsg();
            msg22.setAccountUuid("not exist account uuid");
            msg22.setLdapUid("Not exist");
            msg22.setSession(session);
            APICreateLdapBindingEvent evt22 = sender.send(msg22, APICreateLdapBindingEvent.class);
            logger.debug("" + evt22.getInventory().getId());
        } catch (Exception e) {
            logger.trace("bind account with a non-existent uid", e);
        }

        // bind account
        AccountInventory ai1 = api.createAccount("ldapuser1", "hello-kitty");
        APICreateLdapBindingMsg msg2 = new APICreateLdapBindingMsg();
        msg2.setAccountUuid(ai1.getUuid());
        msg2.setLdapUid("sclaus");
        msg2.setSession(session);
        APICreateLdapBindingEvent evt2 = sender.send(msg2, APICreateLdapBindingEvent.class);
        logger.debug("" + evt2.getInventory().getId());

        // bind another account with the same uid
        try {
            AccountInventory ai12 = api.createAccount("ldapuser2", "hello-kitty");
            APICreateLdapBindingMsg msg22 = new APICreateLdapBindingMsg();
            msg22.setAccountUuid(ai12.getUuid());
            msg22.setLdapUid("sclaus");
            msg22.setSession(session);
            APICreateLdapBindingEvent evt22 = sender.send(msg22, APICreateLdapBindingEvent.class);
            logger.debug("" + evt22.getInventory().getId());
        } catch (Exception e) {
            logger.trace("bind account the same uid", e);
        }


        // login with right ldap uid and right ldap password
        APILogInMsg msg3 = new APILogInMsg();
        msg3.setUsername("sclaus");
        msg3.setPassword("password");
        msg3.setLoginType(LdapConstant.LOGIN_TYPE);
        msg3.setServiceId(bus.makeLocalServiceId(LdapConstant.SERVICE_ID));
        APILogInReply reply3 = sender.call(msg3, APILogInReply.class);
        logger.debug(reply3.getInventory().getAccountUuid());

        // login with right ldap uid and wrong ldap password
        try {
            APILogInMsg msg31 = new APILogInMsg();
            msg31.setUsername("sclaus");
            msg31.setPassword("wrong password");
            msg31.setLoginType(LdapConstant.LOGIN_TYPE);
            msg31.setServiceId(bus.makeLocalServiceId(LdapConstant.SERVICE_ID));
            APILogInReply reply31 = sender.call(msg31, APILogInReply.class);
            logger.debug(reply31.getInventory().getAccountUuid());
        } catch (Exception e) {

        }

        // login with wrong ldap uid
        try {
            APILogInMsg msg31 = new APILogInMsg();
            msg31.setUsername("wrong ldap uid");
            msg31.setPassword("wrong password");
            msg31.setLoginType(LdapConstant.LOGIN_TYPE);
            msg31.setServiceId(bus.makeLocalServiceId(LdapConstant.SERVICE_ID));
            APILogInReply reply31 = sender.call(msg31, APILogInReply.class);
            logger.debug(reply31.getInventory().getAccountUuid());
        } catch (Exception e) {

        }

        // unbind account
        APIDeleteLdapBindingMsg msg4 = new APIDeleteLdapBindingMsg();
        msg4.setAccountUuid("" + evt2.getInventory().getAccountUuid());
        msg4.setSession(session);
        APIDeleteLdapBindingEvent evt4 = sender.send(msg4, APIDeleteLdapBindingEvent.class);
        Assert.assertTrue(evt4.getError() == null);
    }
}
