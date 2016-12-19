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

public class TestLdapCleanInvalidBindings {
    CLogger logger = Utils.getLogger(TestLdapCleanInvalidBindings.class);

    public static final String DOMAIN_DSN = "dc=example,dc=com";

    @Rule
    public EmbeddedLdapRule embeddedLdapRule = EmbeddedLdapRuleBuilder.newInstance().bindingToPort(1888).
            usingDomainDsn(DOMAIN_DSN).importingLdifs("users-import.ldif").build();

    @Rule
    public EmbeddedLdapRule embeddedLdapRule2 = EmbeddedLdapRuleBuilder.newInstance().bindingToPort(1889).
            usingDomainDsn(DOMAIN_DSN).importingLdifs("users-import-2.ldif").build();

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
        APIAddLdapServerMsg apiAddLdapServerMsg = new APIAddLdapServerMsg();
        apiAddLdapServerMsg.setName("miao");
        apiAddLdapServerMsg.setDescription("miao desc");
        apiAddLdapServerMsg.setUrl("ldap://localhost:1888");
        apiAddLdapServerMsg.setBase(DOMAIN_DSN);
        apiAddLdapServerMsg.setUsername("");
        apiAddLdapServerMsg.setPassword("");
        apiAddLdapServerMsg.setEncryption("None");
        apiAddLdapServerMsg.setSession(session);
        APIAddLdapServerEvent apiAddLdapServerEvent = sender.send(apiAddLdapServerMsg, APIAddLdapServerEvent.class);
        logger.debug(apiAddLdapServerEvent.getInventory().getName());
        queryLdapServer();

        // create account
        AccountInventory accInv_ldap_1 = api.createAccount("ldapuser1", "hello-kitty");
        AccountInventory accInv_ldap_2 = api.createAccount("ldapuser2", "hello-kitty");
        AccountInventory accInv_3 = api.createAccount("user3", "hello-kitty");
        AccountInventory accInv_4 = api.createAccount("user4", "hello-kitty");

        // bind account
        APICreateLdapBindingMsg msg2 = new APICreateLdapBindingMsg();
        msg2.setAccountUuid(accInv_ldap_1.getUuid());
        msg2.setLdapUid("sclaus");
        msg2.setSession(session);
        APICreateLdapBindingEvent evt2 = sender.send(msg2, APICreateLdapBindingEvent.class);
        logger.debug(evt2.getInventory().getUuid());

        // bind account
        APICreateLdapBindingMsg msg21 = new APICreateLdapBindingMsg();
        msg21.setAccountUuid(accInv_ldap_2.getUuid());
        msg21.setLdapUid("jsteinbeck");
        msg21.setSession(session);
        APICreateLdapBindingEvent evt21 = sender.send(msg21, APICreateLdapBindingEvent.class);
        logger.debug(evt21.getInventory().getUuid());

        // update ldap server
        APIUpdateLdapServerMsg updateMsg1 = new APIUpdateLdapServerMsg();
        updateMsg1.setLdapServerUuid(apiAddLdapServerEvent.getInventory().getUuid());
        updateMsg1.setUrl("ldap://localhost:1889");
        updateMsg1.setSession(session);
        APIUpdateLdapServerEvent updateEvt1 = sender.send(updateMsg1, APIUpdateLdapServerEvent.class);

        // clean invalid bindings
        APICleanInvalidLdapBindingMsg cleanMsg1 = new APICleanInvalidLdapBindingMsg();
        cleanMsg1.setSession(session);
        APICleanInvalidLdapBindingEvent cleanEvt1 = sender.send(cleanMsg1, APICleanInvalidLdapBindingEvent.class);

        // some assertions
        Assert.assertTrue(cleanEvt1.getAccountInventories().size() == 1);
        Assert.assertTrue(cleanEvt1.getAccountInventories().get(0).getUuid().equals(accInv_ldap_2.getUuid()));
    }
}
