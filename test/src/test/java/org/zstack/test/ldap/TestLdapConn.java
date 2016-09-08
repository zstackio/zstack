package org.zstack.test.ldap;

import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.ldap.APITestAddLdapServerConnectionEvent;
import org.zstack.ldap.APITestAddLdapServerConnectionMsg;
import org.zstack.ldap.LdapManager;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;


public class TestLdapConn {
    CLogger logger = Utils.getLogger(TestLdapConn.class);

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
        deployer = new Deployer("deployerXml/ldap/TestLdapConn.xml", con);
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

    @Test
    public void test() throws ApiSenderException {
        ApiSender sender = api.getApiSender();

        // test conn
        APITestAddLdapServerConnectionMsg msg21 = new APITestAddLdapServerConnectionMsg();
        msg21.setName("miao");
        msg21.setDescription("miao desc");
        msg21.setUrl("ldap://172.20.12.176:389");
        msg21.setBase("dc=learnitguide,dc=net");
        msg21.setUsername("cn=Manager,dc=learnitguide,dc=net");
        msg21.setPassword("password");
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
