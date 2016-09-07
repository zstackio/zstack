package org.zstack.test.ldap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.host.APIAddHostEvent;
import org.zstack.header.identity.APILogInReply;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.ldap.*;
import org.zstack.network.service.eip.APIQueryEipMsg;
import org.zstack.network.service.eip.APIQueryEipReply;
import org.zstack.network.service.eip.EipInventory;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.simulator.virtualrouter.VirtualRouterSimulatorConfig;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.search.QueryTestValidator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * @author frank
 */
public class TestLdap {
    CLogger logger = Utils.getLogger(TestLdap.class);

    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    VirtualRouterSimulatorConfig vconfig;
    KVMSimulatorConfig kconfig;
    LdapManager ldapManager;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/ldap/TestLdap.xml", con);
        deployer.addSpringConfig("VirtualRouter.xml");
        deployer.addSpringConfig("VirtualRouterSimulator.xml");
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("vip.xml");
        deployer.addSpringConfig("eip.xml");
        deployer.addSpringConfig("LdapManagerImpl.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        vconfig = loader.getComponent(VirtualRouterSimulatorConfig.class);
        kconfig = loader.getComponent(KVMSimulatorConfig.class);
        ldapManager = loader.getComponent(LdapManager.class);
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws ApiSenderException {
//        LdapServerVO ldapServerVO = new LdapServerVO();
//        ldapServerVO.setUuid(Platform.getUuid());
//        ldapServerVO.setName("miao");
//        ldapServerVO.setUrl("ldap://172.20.12.176:389");
//        ldapServerVO.setBase("dc=learnitguide,dc=net");
//        ldapServerVO.setUsername("");
//        ldapServerVO.setPassword("");
//        dbf.persistAndRefresh(ldapServerVO);

        ApiSender sender = api.getApiSender();

        // add ldap server
        APIAddLdapServerMsg msg1 = new APIAddLdapServerMsg();
        msg1.setName("miao");
        msg1.setDescription("miao desc");
        msg1.setUrl("ldap://172.20.12.176:389");
        msg1.setBase("dc=learnitguide,dc=net");
        msg1.setUsername("");
        msg1.setPassword("");
        msg1.setSession(session);
        APIAddLdapServerEvent evt1 = sender.send(msg1, APIAddLdapServerEvent.class);
        logger.debug(evt1.getInventory().getName());

        // some assertions
        ldapManager.readLdapServerConfiguration();

        Assert.assertFalse(ldapManager.isValid("ldapuser1", ""));
        Assert.assertFalse(ldapManager.isValid("miao", ""));
        Assert.assertTrue(ldapManager.isValid("ldapuser1", "redhat"));
        Assert.assertTrue(ldapManager.isValid("admin", "miao"));

        // bind account
        AccountInventory ai1 = api.createAccount("ldapuser1", "hello-kitty");
        APIBindLdapAccountMsg msg2 = new APIBindLdapAccountMsg();
        msg2.setAccountUuid(ai1.getUuid());
        msg2.setLdapUid("ldapuser1");
        msg2.setSession(session);
        APIBindLdapAccountEvent evt2 = sender.send(msg2, APIBindLdapAccountEvent.class);
        logger.debug(evt2.getInventory().getUuid());

        // login account
        APILoginByLdapMsg msg3 = new APILoginByLdapMsg();
        msg3.setUid("ldapuser1");
        msg3.setPassword("redhat");
        msg3.setServiceId(bus.makeLocalServiceId(LdapConstant.SERVICE_ID));
        APILogInReply reply3 = sender.call(msg3, APILogInReply.class);
        logger.debug(reply3.getInventory().getAccountUuid());
    }
}
