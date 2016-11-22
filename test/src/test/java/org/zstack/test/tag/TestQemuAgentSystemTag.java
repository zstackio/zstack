package org.zstack.test.tag;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.image.APIQueryImageMsg;
import org.zstack.header.image.APIQueryImageReply;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageVO;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.query.QueryOp;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.header.tag.TagInventory;
import org.zstack.header.vm.APIQueryVmInstanceMsg;
import org.zstack.header.vm.APIQueryVmInstanceReply;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.tag.SystemTag;
import org.zstack.tag.TagSubQueryExtension;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class TestQemuAgentSystemTag {
    protected static final CLogger logger = Utils.getLogger(TestQemuAgentSystemTag.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @TagDefinition
    public static class TestSystemTags {
        public static SystemTag qemu = new SystemTag("qemuga", ImageVO.class);
        public static SystemTag qemu_failed = new SystemTag("qemuga::", ImageVO.class);
    }

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/tag/TestQemuAgentTag.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException {
        ImageInventory image1 = deployer.images.get("Image_1");
        TagInventory inv =  api.createSystemTag(image1.getUuid(), TestSystemTags.qemu.getTagFormat(), ImageVO.class);

        APIQueryImageMsg imsg = new APIQueryImageMsg();
        imsg.addQueryCondition(TagSubQueryExtension.SYS_TAG_NAME, QueryOp.EQ, TestSystemTags.qemu.getTagFormat());
        APIQueryImageReply ireply = api.query(imsg, APIQueryImageReply.class);
        List<ImageInventory> images = ireply.getInventories();
        Assert.assertEquals(1, images.size());
        ImageInventory iinv = images.get(0);
        Assert.assertEquals(image1.getUuid(), iinv.getUuid());
        Assert.assertEquals(TestSystemTags.qemu.getTag(iinv.getUuid()), TestSystemTags.qemu.getTagFormat());

        // make sure vm and image have the same system-tags
        VmInstanceInventory testvm = createVmFromImage(deployer.vms.get("Vm_1"));
        APIQueryVmInstanceMsg vmsg = new APIQueryVmInstanceMsg();
        vmsg.addQueryCondition(TagSubQueryExtension.SYS_TAG_NAME, QueryOp.EQ, TestSystemTags.qemu.getTagFormat());
        APIQueryVmInstanceReply vreply = api.query(vmsg, APIQueryVmInstanceReply.class);
        List<VmInstanceInventory> vms = vreply.getInventories();
        Assert.assertEquals(1, vms.size());
        VmInstanceInventory vinv = vms.get(0);
        Assert.assertEquals(testvm.getUuid(), vinv.getUuid());

        // make sure vm take the SystemTags while clone
//        VmInstanceInventory clonevm = createVmFromImage();


        api.deleteTag(inv.getUuid());

        SystemTagVO tvo = dbf.findByUuid(inv.getUuid(), SystemTagVO.class);
        Assert.assertNull(tvo);

//        inv =  api.createSystemTag(image1.getUuid(), TestSystemTags.qemu_failed.getTagFormat(), ImageVO.class);
//        Assert.assertNull(inv);
    }

    VmInstanceInventory createVmFromImage(VmInstanceInventory vm) throws ApiSenderException {
        List<DiskOfferingInventory> dinvs = api.listDiskOffering(null);
        List<L3NetworkInventory> nwinvs = api.listL3Network(null);
        List<String> nws = new ArrayList<String>(nwinvs.size());
        Assert.assertNotNull(vm);
        nws.add(vm.getDefaultL3NetworkUuid());
        List <String> disks = new ArrayList<String>(1);
        disks.add(dinvs.get(1).getUuid());
        Assert.assertNotNull(dinvs.get(0));
        Assert.assertNotNull(nws.get(0));
        Assert.assertNotNull(disks.get(0));
        VmInstanceInventory testvm = api.createVmByFullConfig(vm, dinvs.get(0).getUuid(), nws, disks);
        return testvm;
    }
}
