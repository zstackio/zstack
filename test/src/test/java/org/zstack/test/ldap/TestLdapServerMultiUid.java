package org.zstack.test.ldap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.query.QueryCondition;
import org.zstack.ldap.*;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class TestLdapServerMultiUid {
    CLogger logger = Utils.getLogger(TestLdapServerMultiUid.class);

    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    KVMSimulatorConfig kconfig;
    LdapManager ldapManager;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/ldap/TestLdap.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("LdapManagerImpl.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        ldapManager = loader.getComponent(LdapManager.class);
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
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
    public void test() throws ApiSenderException {
        ApiSender sender = api.getApiSender();

        // add ldap server
        APIAddLdapServerMsg msg1 = new APIAddLdapServerMsg();
        msg1.setName("miao");
        msg1.setDescription("miao desc");
        msg1.setUrl("ldap://172.20.11.200:389");
        msg1.setBase("cn=accounts,dc=mevoco,dc=com");
        msg1.setUsername("");
        msg1.setPassword("");
        msg1.setSession(session);
        APIAddLdapServerEvent evt1 = sender.send(msg1, APIAddLdapServerEvent.class);
        logger.debug(evt1.getInventory().getName());
        queryLdapServer();

        // some assertions
        Assert.assertFalse(ldapManager.isValid("ldapuser1", ""));
        Assert.assertFalse(ldapManager.isValid("miao", ""));
        Assert.assertTrue(ldapManager.isValid("star.guo", "password"));
        Assert.assertTrue(ldapManager.isValid("admin", "password"));
    }
}
