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
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.ldap.APITestAddLdapServerConnectionEvent;
import org.zstack.ldap.APITestAddLdapServerConnectionMsg;
import org.zstack.ldap.LdapManager;
import org.zstack.test.Api;
import org.zstack.test.ApiSender;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;


public class TestLdapConn {
    CLogger logger = Utils.getLogger(TestLdapConn.class);

    public static final String DOMAIN_DSN = "dc=example,dc=com";
    @Rule
    public EmbeddedLdapRule embeddedLdapRule = EmbeddedLdapRuleBuilder.newInstance().bindingToPort(1888).
            usingDomainDsn(DOMAIN_DSN).importingLdifs("users-import.ldif").build();

    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    LdapManager ldapManager;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();

        deployer = new Deployer("deployerXml/ldap/TestLdapConn.xml");
        deployer.addSpringConfig("LdapManagerImpl.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        ldapManager = loader.getComponent(LdapManager.class);
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException, LDAPException {
        final LDAPInterface ldapConnection = embeddedLdapRule.ldapConnection();
        final SearchResult searchResult = ldapConnection.search(DOMAIN_DSN, SearchScope.SUB, "(objectClass=person)");
        Assert.assertEquals(3, searchResult.getEntryCount());

        ApiSender sender = api.getApiSender();

        // test conn
        APITestAddLdapServerConnectionMsg msg21 = new APITestAddLdapServerConnectionMsg();
        msg21.setName("miao");
        msg21.setDescription("miao desc");
        msg21.setUrl("ldap://localhost:1888");
        msg21.setBase(DOMAIN_DSN);
        msg21.setUsername("");
        msg21.setPassword("");
        msg21.setSession(session);
        msg21.setTimeout(10);
        APITestAddLdapServerConnectionEvent evt21 = sender.send(msg21, APITestAddLdapServerConnectionEvent.class);
        logger.debug(evt21.getInventory().getName());


        // test conn
        APITestAddLdapServerConnectionMsg msg22 = new APITestAddLdapServerConnectionMsg();
        msg22.setName("miao");
        msg22.setDescription("miao desc");
        msg22.setUrl("ldap://172.20.11.200:389");
        msg22.setBase("dc=mevoco,dc=com");
        msg22.setUsername("");
        msg22.setPassword("");
        msg22.setSession(session);
        msg22.setTimeout(10);
        APITestAddLdapServerConnectionEvent evt22 = sender.send(msg22, APITestAddLdapServerConnectionEvent.class);
        logger.debug(evt22.getInventory().getName());

    }
}
