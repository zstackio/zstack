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
        int flag = 1;
        logger.debug("Execute Path:" + flag);
        switch (flag) {
            case 0: {
                APITestAddLdapServerConnectionMsg msg21 = new APITestAddLdapServerConnectionMsg();
                msg21.setName("miao");
                msg21.setDescription("miao desc");
                msg21.setUrl("ldap://localhost:1888");
                msg21.setBase(DOMAIN_DSN);
                msg21.setUsername("");
                msg21.setPassword("");
                msg21.setEncryption("None");
                msg21.setSession(session);
                msg21.setTimeout(10);
                APITestAddLdapServerConnectionEvent evt21 = sender.send(msg21, APITestAddLdapServerConnectionEvent.class);
                logger.debug(evt21.getInventory().getName());
            }
            break;
            case 1: {
                APITestAddLdapServerConnectionMsg msg33 = new APITestAddLdapServerConnectionMsg();
                msg33.setName("miao");
                msg33.setDescription("miao desc");
                msg33.setUrl("ldap://172.20.11.200:389");
                msg33.setBase("dc=mevoco,dc=com");
                msg33.setUsername("uid=admin,cn=users,cn=accounts,dc=mevoco,dc=com");
                msg33.setPassword("password");
                msg33.setEncryption("TLS");
                msg33.setSession(session);
                msg33.setTimeout(10);
                APITestAddLdapServerConnectionEvent evt33 = sender.send(msg33, APITestAddLdapServerConnectionEvent.class);
                logger.debug(evt33.getInventory().getName());
            }
            break;
            case 2: {
                APITestAddLdapServerConnectionMsg msg33 = new APITestAddLdapServerConnectionMsg();
                msg33.setName("miao");
                msg33.setDescription("miao desc");
                msg33.setUrl("ldap://172.20.11.200:389");
                msg33.setBase("dc=mevoco,dc=com");
                msg33.setUsername("uid=admin,cn=users,cn=accounts,dc=mevoco,dc=com");
                msg33.setPassword("password");
                msg33.setEncryption("None");
                msg33.setSession(session);
                msg33.setTimeout(10);
                APITestAddLdapServerConnectionEvent evt33 = sender.send(msg33, APITestAddLdapServerConnectionEvent.class);
                logger.debug(evt33.getInventory().getName());
            }
            break;
            case 3: {
                APITestAddLdapServerConnectionMsg msg44 = new APITestAddLdapServerConnectionMsg();
                msg44.setName("miao");
                msg44.setDescription("miao desc");
                msg44.setUrl("ldap://172.20.12.176:389");
                msg44.setBase("dc=learnitguide,dc=net");
                msg44.setUsername("cn=Manager,dc=learnitguide,dc=net");
                msg44.setPassword("password");
                msg44.setEncryption("None");
                msg44.setSession(session);
                msg44.setTimeout(10);
                APITestAddLdapServerConnectionEvent evt44 = sender.send(msg44, APITestAddLdapServerConnectionEvent.class);
                logger.debug(evt44.getInventory().getName());
            }
            break;
            case 4: {
                APITestAddLdapServerConnectionMsg msg22 = new APITestAddLdapServerConnectionMsg();
                msg22.setName("miao");
                msg22.setDescription("miao desc");
                msg22.setUrl("ldaps://172.20.11.200:389");
                msg22.setBase("dc=mevoco,dc=com");
                msg22.setUsername("uid=admin,cn=users,cn=accounts,dc=mevoco,dc=com");
                msg22.setPassword("password");
                msg22.setEncryption("None");
                msg22.setSession(session);
                msg22.setTimeout(10);
                APITestAddLdapServerConnectionEvent evt22 = sender.send(msg22, APITestAddLdapServerConnectionEvent.class);
                logger.debug(evt22.getInventory().getName());
            }
            break;
        }


    }
}
