package org.zstack.test.mevoco.qos;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.vm.APIDeleteNicQosEvent;
import org.zstack.header.vm.APIGetNicQosReply;
import org.zstack.header.vm.APISetNicQosEvent;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created by mingjian.deng on 16/12/20.
 */

/**
 * mixed with instance_offering and volume_qos
 * linked with: https://github.com/zxwing/functional-spec/issues/7
 * 1. create a vm with qos
 * 2. assert the qos as instance_offering
 * 3. set out and assert it
 * 4. set in and out and assert it
 * 5. delete in and assert it reset to qos as instance_offering
 * 6. delete out and assert it reset to qos as instance_offering
 * 7. delete out again and assert it set to 0
 * 8. reboot vm and assert it back to qos as instance_offering
 */
public class TestNicQosMixed {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    protected static final CLogger logger = Utils.getLogger(TestNicQosMixed.class);


    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/kvm/TestCreateVmOnKvmForQos.xml");
        deployer.addSpringConfig("mevocoRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException {
        //1.
        String nicUuid = deployer.vms.get("Vm_1").getVmNics().get(0).getUuid();
        VmInstanceInventory vm = deployer.vms.get("Vm_1");
        //2.
        APIGetNicQosReply reply = api.getVmNicQos(nicUuid);
        Assert.assertTrue(reply.isSuccess());
        Assert.assertEquals(4096000l, reply.getInboundBandwidth());
        Assert.assertEquals(2048000l, reply.getOutboundBandwidth());
        //3.
        api.setVmNicQos(nicUuid, 204800l);
        reply = api.getVmNicQos(nicUuid);
        Assert.assertTrue(reply.isSuccess());
        Assert.assertEquals(4096000l, reply.getInboundBandwidth());
        Assert.assertEquals(204800l, reply.getOutboundBandwidth());
        //4.
        APISetNicQosEvent event = api.setVmNicQos(nicUuid, 1024000l, 409600l);
        Assert.assertTrue(event.isSuccess());
        reply = api.getVmNicQos(nicUuid);
        Assert.assertTrue(reply.isSuccess());
        Assert.assertEquals(1024000l, reply.getInboundBandwidth());
        Assert.assertEquals(409600l, reply.getOutboundBandwidth());
        //5.

        APIDeleteNicQosEvent event1;
        try {
            event1 = api.deleteVmNicQos(nicUuid, "net");
        } catch(ApiSenderException e) {
            Assert.assertEquals(SysErrors.INVALID_ARGUMENT_ERROR.toString(), e.getError().getCode());
        }
        event1 = api.deleteVmNicQos(nicUuid, "in");
        Assert.assertTrue(event1.isSuccess());
        reply = api.getVmNicQos(nicUuid);
        Assert.assertTrue(reply.isSuccess());
        Assert.assertEquals(4096000l, reply.getInboundBandwidth());
        Assert.assertEquals(409600l, reply.getOutboundBandwidth());
        //6.
        event1 = api.deleteVmNicQos(nicUuid, "out");
        Assert.assertTrue(event1.isSuccess());
        reply = api.getVmNicQos(nicUuid);
        Assert.assertTrue(reply.isSuccess());
        Assert.assertEquals(4096000l, reply.getInboundBandwidth());
        Assert.assertEquals(2048000l, reply.getOutboundBandwidth());
        //7. TODO get -1 before reboot
//        event1 = api.deleteVmNicQos(nicUuid, "out");
//        Assert.assertTrue(event1.isSuccess());
//        reply = api.getVmNicQos(nicUuid);
//        Assert.assertTrue(reply.isSuccess());
//        Assert.assertEquals(4000l, reply.getInboundBandwidth());
//        Assert.assertEquals(0l, reply.getOutboundBandwidth());

        //8.
        api.rebootVmInstance(vm.getUuid());
        reply = api.getVmNicQos(nicUuid);
        Assert.assertTrue(reply.isSuccess());
        Assert.assertEquals(4096000l, reply.getInboundBandwidth());
        Assert.assertEquals(2048000l, reply.getOutboundBandwidth());

    }
}
