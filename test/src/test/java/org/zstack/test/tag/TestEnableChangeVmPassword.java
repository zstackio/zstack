package org.zstack.test.tag;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.tag.APIEnableChangeVmPasswordEvent;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

/**
 * Created by mingjian.deng on 16/12/15.
 */
public class TestEnableChangeVmPassword {
    protected static final CLogger logger = Utils.getLogger(TestEnableChangeVmPassword.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/tag/TestEnableChangeVmPassword.xml", con);
        deployer.addSpringConfig("tag.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        TestQemuAgentSystemTag.TestSystemTags.qemu.getTagFormat();
    }

    @Test
    public void test() throws ApiSenderException {
        ImageInventory image1 = deployer.images.get("Image_1");
        VmInstanceInventory vm1 = deployer.vms.get("Vm_1");
        APIEnableChangeVmPasswordEvent evt = api.enableChangeVmPassword(image1.getUuid(), "ImageVO", true);
        Assert.assertTrue(evt.isSuccess());
        Assert.assertTrue(evt.isEnable());
        Assert.assertEquals(evt.getResourceUuid(), image1.getUuid());

        evt = api.enableChangeVmPassword(image1.getUuid(), "ImageVO", false);
        Assert.assertTrue(evt.isSuccess());
        Assert.assertFalse(evt.isEnable());
        Assert.assertEquals(evt.getResourceUuid(), image1.getUuid());

        try {
            api.enableChangeVmPassword(image1.getUuid(), "VmInstanceVO", true);
            Assert.assertTrue("resourceType_VmInstanceVO_not_support_resourceUuid_image", false);
        } catch (ApiSenderException e) {
            Assert.assertEquals(e.getError().getCode(), SysErrors.OPERATION_ERROR.toString());
        }

        evt = api.enableChangeVmPassword(vm1.getUuid(), "VmInstanceVO", true);
        Assert.assertTrue(evt.isSuccess());
        Assert.assertTrue(evt.isEnable());
        Assert.assertEquals(evt.getResourceUuid(), vm1.getUuid());

        try {
            api.enableChangeVmPassword(vm1.getUuid(), "ImageVO", true);
            Assert.assertTrue("resourceType_ImageVO_not_support_resourceUuid_vm", false);
        } catch (ApiSenderException e) {
            Assert.assertEquals(e.getError().getCode(), SysErrors.OPERATION_ERROR.toString());
        }

        try {
            api.enableChangeVmPassword(vm1.getUuid(), "VolumeVO", true);
            Assert.assertTrue("only_support_VmInstanceVO_and_ImageVO", false);
        } catch (ApiSenderException e) {
            Assert.assertEquals(e.getError().getCode(), SysErrors.INVALID_ARGUMENT_ERROR.toString());
        }

        try {
            api.enableChangeVmPassword("test_must_exception", "ImageVO", true);
            Assert.assertTrue("resource_uuid_must_find", false);
        } catch (ApiSenderException e) {
            Assert.assertEquals(e.getError().getCode(), SysErrors.OPERATION_ERROR.toString());
        }
    }
}
