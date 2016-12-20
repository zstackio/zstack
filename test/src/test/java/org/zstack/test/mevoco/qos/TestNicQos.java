package org.zstack.test.mevoco.qos;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.vm.APIGetVmNicQosReply;
import org.zstack.header.vm.APISetVmNicQosEvent;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
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
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/vm/TestCreateVm.xml", con);
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
            api.setVmNicQos(uuid, nicUuid, 1023l, 2048000l);
            Assert.assertTrue("inbound/outbound must more than 1024", false);
        } catch(ApiSenderException e) {
            Assert.assertEquals(SysErrors.INVALID_ARGUMENT_ERROR.toString(), e.getError().getCode());
        }

        APISetVmNicQosEvent evt = api.setVmNicQos(uuid, nicUuid, 1024l, 2048000l);
        Assert.assertTrue(evt.isSuccess());

        APIGetVmNicQosReply reply = api.getVmNicQos(uuid, nicUuid);
        Assert.assertTrue(reply.isSuccess());


        Assert.assertEquals(1024l, reply.getVmNicQOS().getInboundBandwidth());
        Assert.assertEquals(2048000l, reply.getVmNicQOS().getOutboundBandwidth());

    }
}
