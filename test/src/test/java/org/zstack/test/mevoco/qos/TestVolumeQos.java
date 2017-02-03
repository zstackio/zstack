package org.zstack.test.mevoco.qos;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.vm.CloneVmInstanceResults;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.APIDeleteVolumeQosEvent;
import org.zstack.header.volume.APIGetVolumeQosReply;
import org.zstack.header.volume.APISetVolumeQosEvent;
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
public class TestVolumeQos {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    VmCreator creator;

    protected static final CLogger logger = Utils.getLogger(TestVolumeQos.class);


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
        creator = new VmCreator(api);
    }

    @Test
    public void test() throws ApiSenderException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        String rootVolumeUuid = vm.getRootVolumeUuid();
        List<String> names = new ArrayList<>();
        names.add("test1");
        // 1. set the qos and check it
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

        // 2. clone it and check the qos as origin vm
        CloneVmInstanceResults res = creator.cloneVm(names, vm.getUuid());
        reply = api.getVmDiskQos(res.getInventories().get(0).getInventory().getRootVolumeUuid());
        Assert.assertTrue(reply.isSuccess());
        Assert.assertEquals(1024l, reply.getVolumeBandwidth());

        // 3. delete the qos and check it
        APIDeleteVolumeQosEvent event = api.deleteDiskQos(rootVolumeUuid);
        Assert.assertTrue(event.isSuccess());

        reply = api.getVmDiskQos(rootVolumeUuid);
        Assert.assertEquals(-1l, reply.getVolumeBandwidth());

        SystemTagVO tvo = dbf.findByUuid(rootVolumeUuid, SystemTagVO.class);
        Assert.assertNull(tvo);

        // 4. clone it and check the qos as origin vm
        res = creator.cloneVm(names, vm.getUuid());
        reply = api.getVmDiskQos(res.getInventories().get(0).getInventory().getRootVolumeUuid());
        Assert.assertTrue(reply.isSuccess());
        Assert.assertEquals(-1l, reply.getVolumeBandwidth());
    }
}
