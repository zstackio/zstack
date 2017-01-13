package org.zstack.test.core;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.encrypt.EncryptManagerImpl;
import org.zstack.core.encrypt.EncryptRSA;
import org.zstack.header.allocator.HostCapacityVO;
import org.zstack.header.allocator.HostCpuOverProvisioningManager;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.core.encrypt.APIUpdateEncryptKeyMsg;
import org.zstack.header.core.encrypt.DECRYPT;
import org.zstack.header.core.encrypt.ENCRYPT;
import org.zstack.header.host.APIAddHostEvent;
import org.zstack.header.core.encrypt.APIUpdateEncryptKeyEvent;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.SessionInventory;
import org.zstack.kvm.APIAddKVMHostMsg;
import org.zstack.kvm.KVMHostFactory;
import org.zstack.kvm.KVMHostVO;
import org.zstack.simulator.kvm.KVMSimulatorConfig;
import org.zstack.test.*;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

/**
 * Created by mingjian.deng on 16/11/2.
 */
public class TestEncrypt {
    private String password;
    ComponentLoader loader;
    EncryptRSA rsa;
    static Api api;
    static CloudBus bus;
    static DatabaseFacade dbf;
    static KVMHostFactory kvmFactory;
    static SessionInventory session;
    static KVMSimulatorConfig config;
    static HostCpuOverProvisioningManager cpuMgr;
    private static final CLogger logger = Utils.getLogger(TestEncrypt.class);
    Deployer deployer;


    @Before
    public void setUp() throws Exception {

        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestAddKvmHost.xml", con);
        deployer.addSpringConfig("Kvm.xml");
        deployer.addSpringConfig("KVMSimulator.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        kvmFactory = loader.getComponent(KVMHostFactory.class);
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(KVMSimulatorConfig.class);
        cpuMgr = loader.getComponent(HostCpuOverProvisioningManager.class);
        session = api.loginAsAdmin();

        BeanConstructor con1 = new BeanConstructor();
        loader = con1.build();
        rsa = loader.getComponent(EncryptRSA.class);

        /*deployer = new Deployer("deployerXml/mevoco/TestMevoco.xml", con);
        deployer.addSpringConfig("mevocoRelated.xml");
        deployer.addSpringConfig("billing.xml");
        deployer.load();

        loader = deployer.getComponentLoader();
        //bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);*/

    }

    private HostInventory addHost() throws ApiSenderException {
        ClusterInventory cinv = api.listClusters(null).get(0);
        APIAddKVMHostMsg msg = new APIAddKVMHostMsg();
        msg.setName("KVM-1");
        msg.setClusterUuid(cinv.getUuid());
        msg.setManagementIp("localhost");
        msg.setUsername("admin");
        msg.setPassword("password1");
        msg.setSession(session);
        ApiSender sender = api.getApiSender();
        APIAddHostEvent evt = sender.send(msg, APIAddHostEvent.class);
        return evt.getInventory();
    }

    private HostInventory addHost1() throws ApiSenderException {
        ClusterInventory cinv = api.listClusters(null).get(1);
        APIAddKVMHostMsg msg = new APIAddKVMHostMsg();
        msg.setName("KVM-2");
        msg.setClusterUuid(cinv.getUuid());
        msg.setManagementIp("127.0.0.1");
        msg.setUsername("admin1");
        msg.setPassword("kkkpppxxx");
        msg.setSession(session);
        ApiSender sender = api.getApiSender();
        APIAddHostEvent evt = sender.send(msg, APIAddHostEvent.class);
        return evt.getInventory();
    }

    private String updateKey() throws ApiSenderException {
        ClusterInventory cinv = api.listClusters(null).get(0);
        APIUpdateEncryptKeyMsg msg = new APIUpdateEncryptKeyMsg();

        msg.setEncryptKey("zDXbBPIDPKVo230xjcqFcg==");
        msg.setSession(session);
        ApiSender sender = api.getApiSender();
        APIUpdateEncryptKeyEvent evt = sender.send(msg, APIUpdateEncryptKeyEvent.class);
        return "success";
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    public void setString(String password){
        this.password = password;
    }


    public String getString(){
        return password;
    }

    @Test
    public void test() throws ApiSenderException {
        /*setString("pwd");
        Assert.assertNotSame("if encrypt successful, this couldn't be same.", "pwd", getPassword());
        String decreptPassword = getString();
        Assert.assertNotNull(decreptPassword);
        Assert.assertEquals("pwd", getString());
        Assert.assertTrue("pwd".equals(decreptPassword));

        setPassword("test_update");
        Assert.assertEquals("test_update", getString());*/

        config.connectSuccess = true;
        config.connectException = false;
        config.hostFactSuccess = true;
        config.hostFactException = false;
        config.cpuNum = 1;
        config.cpuSpeed = 2600;
        config.totalMemory = SizeUnit.GIGABYTE.toByte(8);
        config.usedMemory = SizeUnit.MEGABYTE.toByte(512);

        String uuid = addHost().getUuid();
        String uuid1 = addHost1().getUuid();

        HostCapacityVO hvo = dbf.findByUuid(uuid, HostCapacityVO.class);
        HostCapacityVO hvo1 = dbf.findByUuid(uuid1, HostCapacityVO.class);

        String result = updateKey();
        //logger.debug("result is: "+result);

        //dbf.persist(kvmHostVO);
    }
}
