package org.zstack.test.mevoco.qos;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.vm.APIDeleteNicQosEvent;
import org.zstack.header.vm.APIGetNicQosReply;
import org.zstack.header.vm.APISetNicQosEvent;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created by mingjian.deng on 16/12/20.
 */
public class TestNicQos {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    protected static final CLogger logger = Utils.getLogger(TestNicQos.class);


    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/kvm/TestCreateVmOnKvm.xml");
        deployer.addSpringConfig("mevocoRelated.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException {
        String nicUuid = deployer.vms.get("TestVm").getVmNics().get(0).getUuid();
        String uuid = deployer.vms.get("TestVm").getUuid();

        try {
            api.setVmNicQos(nicUuid, 1023l, 2048000l);
            Assert.assertTrue("inbound/outbound must more than 1024", false);
        } catch(ApiSenderException e) {
            Assert.assertEquals(SysErrors.INVALID_ARGUMENT_ERROR.toString(), e.getError().getCode());
        }

        APISetNicQosEvent evt = api.setVmNicQos(nicUuid, 1024l, 2048000l);
        Assert.assertTrue(evt.isSuccess());

        APIGetNicQosReply reply = api.getVmNicQos(nicUuid);
        Assert.assertTrue(reply.isSuccess());


        Assert.assertEquals(1024l, reply.getInboundBandwidth());
        Assert.assertEquals(2048000l, reply.getOutboundBandwidth());

        evt = api.setVmNicQos(nicUuid, 1024000l);
        Assert.assertTrue(evt.isSuccess());

        reply = api.getVmNicQos(nicUuid);
        Assert.assertTrue(reply.isSuccess());
        Assert.assertEquals(1024000l, reply.getOutboundBandwidth());

        APIDeleteNicQosEvent event = api.deleteVmNicQos(nicUuid, "in");
        Assert.assertTrue(event.isSuccess());

        reply = api.getVmNicQos(nicUuid);
        Assert.assertTrue(reply.isSuccess());
        Assert.assertEquals(1024000l, reply.getOutboundBandwidth());
        Assert.assertEquals(-1l, reply.getInboundBandwidth());

        event = api.deleteVmNicQos(nicUuid, "out");
        Assert.assertTrue(event.isSuccess());
        SystemTagVO tvo = dbf.findByUuid(nicUuid, SystemTagVO.class);
        Assert.assertNull(tvo);

    }
}
