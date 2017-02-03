package org.zstack.test.mevoco.qos;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.vm.*;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageSimulatorConfig;
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
public class TestNicQos {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    VmCreator creator;

    protected static final CLogger logger = Utils.getLogger(TestNicQos.class);


    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/kvm/TestQos.xml");
        deployer.addSpringConfig("mevocoRelated.xml");
        deployer.addSpringConfig("imagestore.xml");
        deployer.addSpringConfig("ImageStoreBackupStorageSimulator.xml");
        deployer.addSpringConfig("ImageStorePrimaryStorageSimulator.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        loader.getComponent(ImageStoreBackupStorageSimulatorConfig.class);
        creator = new VmCreator(api);
    }

    @Test
    public void test() throws ApiSenderException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        String defaultL3Uuid = vm.getDefaultL3NetworkUuid();
        SimpleQuery<VmNicVO> q = dbf.createQuery(VmNicVO.class);
        q.add(VmNicVO_.l3NetworkUuid, SimpleQuery.Op.EQ, defaultL3Uuid);
        q.add(VmNicVO_.vmInstanceUuid, SimpleQuery.Op.EQ, vm.getUuid());
        String srcNicUuid = q.find().getUuid();

        List<String> names = new ArrayList<>();
        names.add("test1");

        // 1. set the qos and check it
        try {
            api.setVmNicQos(srcNicUuid, 8191l, 2048000l);
            Assert.assertTrue("inbound/outbound must more than 1024", false);
        } catch(ApiSenderException e) {
            Assert.assertEquals(SysErrors.INVALID_ARGUMENT_ERROR.toString(), e.getError().getCode());
        }

        APISetNicQosEvent evt = api.setVmNicQos(srcNicUuid, 8192l, 2048000l);
        Assert.assertTrue(evt.isSuccess());

        APIGetNicQosReply reply = api.getVmNicQos(srcNicUuid);
        Assert.assertTrue(reply.isSuccess());

        Assert.assertEquals(8192l, reply.getInboundBandwidth());
        Assert.assertEquals(2048000l, reply.getOutboundBandwidth());

        evt = api.setVmNicQos(srcNicUuid, 1024000l);
        Assert.assertTrue(evt.isSuccess());

        reply = api.getVmNicQos(srcNicUuid);
        Assert.assertTrue(reply.isSuccess());
        Assert.assertEquals(1024000l, reply.getOutboundBandwidth());

        // 2. clone it and check the qos as origin vm
        CloneVmInstanceResults res = creator.cloneVm(names, vm.getUuid());
        q = dbf.createQuery(VmNicVO.class);
        q.add(VmNicVO_.l3NetworkUuid, SimpleQuery.Op.EQ, defaultL3Uuid);
        q.add(VmNicVO_.vmInstanceUuid, SimpleQuery.Op.EQ, res.getInventories().get(0).getInventory().getUuid());
        reply = api.getVmNicQos(q.find().getUuid());
        Assert.assertTrue(reply.isSuccess());
        Assert.assertEquals(8192l, reply.getInboundBandwidth());
        Assert.assertEquals(1024000l, reply.getOutboundBandwidth());

        // 3. delete the qos and check it
        APIDeleteNicQosEvent event = api.deleteVmNicQos(srcNicUuid, "in");
        Assert.assertTrue(event.isSuccess());

        reply = api.getVmNicQos(srcNicUuid);
        Assert.assertTrue(reply.isSuccess());
        Assert.assertEquals(-1l, reply.getInboundBandwidth());
        Assert.assertEquals(1024000l, reply.getOutboundBandwidth());

        event = api.deleteVmNicQos(srcNicUuid, "out");
        Assert.assertTrue(event.isSuccess());
        SystemTagVO tvo = dbf.findByUuid(srcNicUuid, SystemTagVO.class);
        Assert.assertNull(tvo);

        // 4. clone it and check the qos as origin vm
        res = creator.cloneVm(names, vm.getUuid());
        q = dbf.createQuery(VmNicVO.class);
        q.add(VmNicVO_.l3NetworkUuid, SimpleQuery.Op.EQ, defaultL3Uuid);
        q.add(VmNicVO_.vmInstanceUuid, SimpleQuery.Op.EQ, res.getInventories().get(0).getInventory().getUuid());
        reply = api.getVmNicQos(q.find().getUuid());
        Assert.assertTrue(reply.isSuccess());
        Assert.assertEquals(-1l, reply.getInboundBandwidth());
        Assert.assertEquals(-1l, reply.getOutboundBandwidth());
    }
}
