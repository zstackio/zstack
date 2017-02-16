package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.header.vm.*;



/**
 * Created by mingjian.deng on 17/1/4.
 */
public class TestVmQgaEnable {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

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
    public void test() throws ApiSenderException, InterruptedException {
        VmInstanceInventory vm = deployer.vms.get("TestVm");

        APIGetVmQgaEnableReply reply = api.getEnableVmQga(vm.getUuid());
        Assert.assertTrue(reply.isSuccess());
        Assert.assertFalse(reply.isEnable());

        APISetVmQgaEvent evt = api.enableVmQga(vm.getUuid());
        Assert.assertTrue(evt.isSuccess());
        String tag = getResourceUuidTag(vm.getUuid());
        Assert.assertEquals(VmSystemTags.VM_INJECT_QEMUGA_TOKEN, tag);

        reply = api.getEnableVmQga(vm.getUuid());
        Assert.assertTrue(reply.isSuccess());
        Assert.assertTrue(reply.isEnable());

        APISetVmQgaEvent disableEvent = api.disableVmQga(vm.getUuid());
        Assert.assertTrue(disableEvent.isSuccess());
        tag = getResourceUuidTag(vm.getUuid());
        Assert.assertNull(tag);

        reply = api.getEnableVmQga(vm.getUuid());
        Assert.assertTrue(reply.isSuccess());
        Assert.assertFalse(reply.isEnable());
    }

    String getResourceUuidTag(String resourceUuid) {
        SimpleQuery<SystemTagVO> pq = dbf.createQuery(SystemTagVO.class);
        pq.select(SystemTagVO_.tag);
        pq.add(SystemTagVO_.resourceUuid, SimpleQuery.Op.EQ, resourceUuid);
        String tag = pq.findValue();
        return tag;
    }
}
