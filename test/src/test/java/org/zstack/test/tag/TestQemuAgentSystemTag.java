package org.zstack.test.tag;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.image.APIQueryImageMsg;
import org.zstack.header.image.APIQueryImageReply;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageVO;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.query.QueryOp;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.tag.TagDefinition;
import org.zstack.header.tag.TagInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.tag.SystemTag;
import org.zstack.tag.TagSubQueryExtension;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collections;
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
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/tag/TestQemuAgentTag.xml", con);
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException {
        ImageInventory image1 = deployer.images.get("Image_1");

        TagInventory inv = api.createSystemTag(image1.getUuid(), TestSystemTags.qemu.getTagFormat(), ImageVO.class);

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
        String tag = getResourceUuidTag(testvm.getUuid());
        Assert.assertEquals(tag, TestSystemTags.qemu.getTagFormat());

        // make sure vm and the generate-image carry off the SystemTags while clone vm
        VmInstanceInventory clonevm = createVmFromClone(deployer.vms.get("Vm_1"));
        tag = getResourceUuidTag(clonevm.getUuid());
        // check the vm have tag
        Assert.assertEquals(TestSystemTags.qemu.getTagFormat(), tag);


        SimpleQuery<VmInstanceVO> pqv = dbf.createQuery(VmInstanceVO.class);
        pqv.add(VmInstanceVO_.uuid, SimpleQuery.Op.EQ, clonevm.getUuid());
        VmInstanceVO cloned = pqv.find();
        String rootImageUuid = cloned.getRootVolume().getRootImageUuid();
        // check the image have tag
        tag = getResourceUuidTag(rootImageUuid);
        Assert.assertEquals(TestSystemTags.qemu.getTagFormat(), tag);


        BackupStorageInventory bs = deployer.backupStorages.get("sftp");
        List<String> bsUuids = Collections.singletonList(bs.getUuid());
        logger.debug(cloned.getRootVolume().getUuid());

//        ImageInventory commitedImage = api.commitVolumeAsImage(testvm.getRootVolumeUuid(), "test-commit-image", bsUuids);
//        // check the commited-image have tag
//        tag = getResourceUuidTag(commitedImage.getUuid());
//        Assert.assertEquals(TestSystemTags.qemu.getTagFormat(), tag);

        api.deleteTag(inv.getUuid());

        SystemTagVO tvo = dbf.findByUuid(inv.getUuid(), SystemTagVO.class);
        Assert.assertNull(tvo);
    }

    String getResourceUuidTag(String resourceUuid) {
        SimpleQuery<SystemTagVO> pq = dbf.createQuery(SystemTagVO.class);
        pq.select(SystemTagVO_.tag);
        pq.add(SystemTagVO_.resourceUuid, SimpleQuery.Op.EQ, resourceUuid);
        String tag = pq.findValue();
        return tag;
    }

    VmInstanceInventory createVmFromImage(VmInstanceInventory vm) throws ApiSenderException {
        List<DiskOfferingInventory> dinvs = api.listDiskOffering(null);
        List<L3NetworkInventory> nwinvs = api.listL3Network(null);
        List<String> nws = new ArrayList<String>(nwinvs.size());
        Assert.assertNotNull(vm);
        nws.add(vm.getDefaultL3NetworkUuid());
        List<String> disks = new ArrayList<String>(1);
        disks.add(dinvs.get(1).getUuid());
        Assert.assertNotNull(dinvs.get(0));
        Assert.assertNotNull(nws.get(0));
        Assert.assertNotNull(disks.get(0));
        VmInstanceInventory testvm = api.createVmByFullConfig(vm, dinvs.get(0).getUuid(), nws, disks);
        return testvm;
    }

    VmInstanceInventory createVmFromClone(VmInstanceInventory vm) throws ApiSenderException {
        return api.createVmFromClone(vm);
    }
}
