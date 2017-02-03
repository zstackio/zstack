package org.zstack.test.mevoco.qos;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.vm.*;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.VmCreator;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

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
 * 5. delete in and assert it reset to qos as -1
 * 6. delete out and assert it reset to qos as -1
 * 7. set in & out qos and clone it, then check it's qos as the initial vm
 */
public class TestNicQosMixed {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    VmCreator creator;
    List<String> names = new ArrayList<>();

    protected static final CLogger logger = Utils.getLogger(TestNicQosMixed.class);


    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/kvm/TestCreateVmOnKvmForQos.xml");
        deployer.addSpringConfig("mevocoRelated.xml");
        deployer.addSpringConfig("imagestore.xml");
        deployer.addSpringConfig("ImageStoreBackupStorageSimulator.xml");
        deployer.addSpringConfig("ImageStorePrimaryStorageSimulator.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        creator = new VmCreator(api);
        names.add("test1");
    }

    @Test
    public void test() throws ApiSenderException {
        //1. create a vm with qos
        VmInstanceInventory vm = deployer.vms.get("Vm_1");

        String defaultL3Uuid = vm.getDefaultL3NetworkUuid();
        SimpleQuery<VmNicVO> q = dbf.createQuery(VmNicVO.class);
        q.add(VmNicVO_.l3NetworkUuid, SimpleQuery.Op.EQ, defaultL3Uuid);
        q.add(VmNicVO_.vmInstanceUuid, SimpleQuery.Op.EQ, vm.getUuid());
        String srcNicUuid = q.find().getUuid();

        //2. assert the qos as instance_offering
        APIGetNicQosReply reply = api.getVmNicQos(srcNicUuid);
        Assert.assertTrue(reply.isSuccess());
        Assert.assertEquals(4096000l, reply.getInboundBandwidth());
        Assert.assertEquals(2048000l, reply.getOutboundBandwidth());

        //3. clone it, then check it's qos as the initial vm
        CloneVmInstanceResults res = creator.cloneVm(names, vm.getUuid());
        VmInstanceInventory clonevm = res.getInventories().get(0).getInventory();
        reply = api.getVmNicQos(clonevm.getVmNics().get(0).getUuid());
        Assert.assertTrue(reply.isSuccess());
        Assert.assertEquals(4096000l, reply.getInboundBandwidth());
        Assert.assertEquals(2048000l, reply.getOutboundBandwidth());

        //4. set qos and check it
        api.setVmNicQos(srcNicUuid, 204800l);
        reply = api.getVmNicQos(srcNicUuid);
        Assert.assertTrue(reply.isSuccess());
        Assert.assertEquals(4096000l, reply.getInboundBandwidth());
        Assert.assertEquals(204800l, reply.getOutboundBandwidth());

        APISetNicQosEvent event = api.setVmNicQos(srcNicUuid, 1024000l, 409600l);
        Assert.assertTrue(event.isSuccess());
        reply = api.getVmNicQos(srcNicUuid);
        Assert.assertTrue(reply.isSuccess());
        Assert.assertEquals(1024000l, reply.getInboundBandwidth());
        Assert.assertEquals(409600l, reply.getOutboundBandwidth());

        //5. clone it, then check it's qos as the initial vm
        res = creator.cloneVm(names, vm.getUuid());
        clonevm = res.getInventories().get(0).getInventory();
        reply = api.getVmNicQos(clonevm.getVmNics().get(0).getUuid());
        Assert.assertTrue(reply.isSuccess());
        Assert.assertEquals(1024000l, reply.getInboundBandwidth());
        Assert.assertEquals(409600l, reply.getOutboundBandwidth());

        //6. delete qos and assert it, it's back to -1

        APIDeleteNicQosEvent event1;
        try {
            api.deleteVmNicQos(srcNicUuid, "net");
        } catch(ApiSenderException e) {
            Assert.assertEquals(SysErrors.INVALID_ARGUMENT_ERROR.toString(), e.getError().getCode());
        }
        event1 = api.deleteVmNicQos(srcNicUuid, "in");
        Assert.assertTrue(event1.isSuccess());
        reply = api.getVmNicQos(srcNicUuid);
        Assert.assertTrue(reply.isSuccess());
        Assert.assertEquals(-1l, reply.getInboundBandwidth());
        Assert.assertEquals(409600l, reply.getOutboundBandwidth());

        event1 = api.deleteVmNicQos(srcNicUuid, "out");
        Assert.assertTrue(event1.isSuccess());
        reply = api.getVmNicQos(srcNicUuid);
        Assert.assertTrue(reply.isSuccess());
        Assert.assertEquals(-1l, reply.getInboundBandwidth());
        Assert.assertEquals(-1l, reply.getOutboundBandwidth());

        //7. clone it, then check it's qos as the initial vm
        res = creator.cloneVm(names, vm.getUuid());
        clonevm = res.getInventories().get(0).getInventory();
        reply = api.getVmNicQos(clonevm.getVmNics().get(0).getUuid());
        Assert.assertTrue(reply.isSuccess());
        Assert.assertEquals(-1l, reply.getInboundBandwidth());
        Assert.assertEquals(-1l, reply.getOutboundBandwidth());
    }
}
