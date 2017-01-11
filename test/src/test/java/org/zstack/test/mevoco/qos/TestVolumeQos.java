package org.zstack.test.mevoco.qos;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.volume.APIGetVolumeQosReply;
import org.zstack.header.volume.APISetVolumeQosEvent;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created by mingjian.deng on 16/12/20.
 */
public class TestVolumeQos {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    protected static final CLogger logger = Utils.getLogger(TestVolumeQos.class);


    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
//        BeanConstructor con = new BeanConstructor();
//        con.addXml("mevocoHostBaseServiceSimulator.xml");
        deployer = new Deployer("deployerXml/vm/TestCreateVm.xml");
        deployer.addSpringConfig("mevocoRelated.xml");
        deployer.addSpringConfig("mevocoHostBaseSimulator.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException {
        String rootVolumeUuid = deployer.vms.get("TestVm").getRootVolumeUuid();
        try {
            api.setDiskQos(rootVolumeUuid, 1023l);
            Assert.assertTrue("bandwidth must more than 1024", false);
        } catch(ApiSenderException e) {
            Assert.assertEquals(SysErrors.INVALID_ARGUMENT_ERROR.toString(), e.getError().getCode());
        }

        APISetVolumeQosEvent evt = api.setDiskQos(rootVolumeUuid, 1024l);
        Assert.assertTrue(evt.isSuccess());

        APIGetVolumeQosReply reply = api.getVmDiskQos(rootVolumeUuid);
        Assert.assertTrue(reply.isSuccess());


        Assert.assertEquals(1024l, reply.getVolumeBandwidth());

    }
}
