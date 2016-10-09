package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.config.GlobalConfigVO;
import org.zstack.core.config.GlobalConfigVO_;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.identity.*;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.image.ImageVO;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.query.QueryOp;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.VmCreator;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.identity.IdentityCreator;
import org.zstack.test.search.QueryTestValidator;
import org.zstack.utils.data.SizeUnit;

import java.util.List;

/**
 * test vm related quota
 */
public class TestPolicyForVm3 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestPolicyForVm3.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        InstanceOfferingInventory ioinv = deployer.instanceOfferings.get("TestInstanceOffering");
        ImageInventory img = deployer.images.get("TestImage");
        L3NetworkInventory l3 = deployer.l3Networks.get("TestL3Network1");
        HostInventory host1 = deployer.hosts.get("TestHost1");
        HostInventory host2 = deployer.hosts.get("TestHost2");

        IdentityCreator identityCreator = new IdentityCreator(api);
        AccountInventory test = identityCreator.useAccount("test");

        List<QuotaVO> quotas = dbf.listAll(QuotaVO.class);
        for (QuotaVO q : quotas) {
            SimpleQuery<GlobalConfigVO> gq = dbf.createQuery(GlobalConfigVO.class);
            gq.add(GlobalConfigVO_.category, Op.EQ, AccountConstant.QUOTA_GLOBAL_CONFIG_CATETORY);
            gq.add(GlobalConfigVO_.name, Op.EQ, q.getName());
            GlobalConfigVO gvo = gq.find();
            Assert.assertEquals(Long.valueOf(gvo.getValue()), Long.valueOf(q.getValue()));
        }

        api.updateQuota(test.getUuid(), VmInstanceConstant.QUOTA_VM_RUNNING_NUM, 0);

        VmCreator vmCreator = new VmCreator(api);
        vmCreator.imageUuid = img.getUuid();
        vmCreator.addL3Network(l3.getUuid());
        vmCreator.instanceOfferingUuid = ioinv.getUuid();
        vmCreator.name = "vm";
        vmCreator.session = identityCreator.getAccountSession();

        // Vm number exceeds
        boolean success = false;
        try {
            vmCreator.create();
        } catch (ApiSenderException e) {
            if (IdentityErrors.QUOTA_EXCEEDING.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        api.updateQuota(test.getUuid(), VmInstanceConstant.QUOTA_VM_RUNNING_NUM, 1);
        VmInstanceInventory vm = vmCreator.create();
        api.destroyVmInstance(vm.getUuid());

        InstanceOfferingInventory cpu6 = deployer.instanceOfferings.get("6cpu");
        vm = vmCreator.create();
        api.destroyVmInstance(vm.getUuid());

        // Vm cpu number exceeds
        api.updateQuota(test.getUuid(), VmInstanceConstant.QUOTA_VM_RUNNING_CPU_NUM, 2);
        success = false;
        try {
            vmCreator.instanceOfferingUuid = cpu6.getUuid();
            vmCreator.create();
        } catch (ApiSenderException e) {
            if (IdentityErrors.QUOTA_EXCEEDING.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        api.updateQuota(test.getUuid(), VmInstanceConstant.QUOTA_VM_RUNNING_CPU_NUM, 100);
        vm = vmCreator.create();
        api.destroyVmInstance(vm.getUuid());

        // Vm cpu memory exceeds
        api.updateQuota(test.getUuid(), VmInstanceConstant.QUOTA_VM_RUNNING_MEMORY_SIZE, SizeUnit.GIGABYTE.toByte(1));
        InstanceOfferingInventory memory12G = deployer.instanceOfferings.get("12G");
        success = false;
        try {
            vmCreator.instanceOfferingUuid = memory12G.getUuid();
            vmCreator.create();
        } catch (ApiSenderException e) {
            if (IdentityErrors.QUOTA_EXCEEDING.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        api.updateQuota(test.getUuid(), VmInstanceConstant.QUOTA_VM_RUNNING_MEMORY_SIZE, SizeUnit.GIGABYTE.toByte(100));
        DiskOfferingInventory disk50G = deployer.diskOfferings.get("disk50G");
        vmCreator.instanceOfferingUuid = ioinv.getUuid();
        vmCreator.addDisk(disk50G.getUuid());
        vm = vmCreator.create();
        api.destroyVmInstance(vm.getUuid());

        // volume size exceeds
        api.updateQuota(test.getUuid(), VolumeConstant.QUOTA_VOLUME_SIZE, SizeUnit.GIGABYTE.toByte(40));
        success = false;
        try {
            vmCreator.create();
        } catch (ApiSenderException e) {
            if (IdentityErrors.QUOTA_EXCEEDING.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        api.updateQuota(test.getUuid(), VolumeConstant.QUOTA_VOLUME_SIZE, SizeUnit.GIGABYTE.toByte(1000));
        vm = vmCreator.create();
        api.destroyVmInstance(vm.getUuid());

        // data volume number exceeds
        api.updateQuota(test.getUuid(), VolumeConstant.QUOTA_DATA_VOLUME_NUM, 1);
        success = false;
        try {
            vmCreator.addDisk(disk50G.getUuid());
            vmCreator.addDisk(disk50G.getUuid());
            vmCreator.addDisk(disk50G.getUuid());
            vmCreator.create();
        } catch (ApiSenderException e) {
            if (IdentityErrors.QUOTA_EXCEEDING.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        api.updateQuota(test.getUuid(), VolumeConstant.QUOTA_DATA_VOLUME_NUM, 10);
        vm = vmCreator.create();
        api.destroyVmInstance(vm.getUuid());

        ImageInventory iso = deployer.images.get("iso");
        DiskOfferingInventory rootDiskOffering = deployer.diskOfferings.get("rootDisk");
        vmCreator = new VmCreator(api);
        vmCreator.instanceOfferingUuid = ioinv.getUuid();
        vmCreator.imageUuid = iso.getUuid();
        vmCreator.addL3Network(l3.getUuid());
        vmCreator.rootDiskOfferingUuid = rootDiskOffering.getUuid();
        vmCreator.name = "vm";
        vmCreator.session = identityCreator.getAccountSession();
        vm = vmCreator.create();
        api.destroyVmInstance(vm.getUuid());

        // volume size exceeds
        api.updateQuota(test.getUuid(), VolumeConstant.QUOTA_VOLUME_SIZE, SizeUnit.GIGABYTE.toByte(5));
        success = false;
        try {
            vmCreator.create();
        } catch (ApiSenderException e) {
            if (IdentityErrors.QUOTA_EXCEEDING.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        ImageVO imgvo = dbf.findByUuid(img.getUuid(), ImageVO.class);
        imgvo.setSize(SizeUnit.GIGABYTE.toByte(50));
        // volume size exceeds
        api.updateQuota(test.getUuid(), VolumeConstant.QUOTA_VOLUME_SIZE, SizeUnit.GIGABYTE.toByte(5));
        success = false;
        try {
            vmCreator.imageUuid = imgvo.getUuid();
            vmCreator.create();
        } catch (ApiSenderException e) {
            if (IdentityErrors.QUOTA_EXCEEDING.toString().equals(e.getError().getCode())) {
                success = true;
            }
        }
        Assert.assertTrue(success);

        APIQueryQuotaMsg msg = new APIQueryQuotaMsg();
        msg.addQueryCondition("identityUuid", QueryOp.EQ, test.getUuid());
        APIQueryQuotaReply reply = api.query(msg, APIQueryQuotaReply.class);
        QuotaInventory quota = reply.getInventories().get(0);

        QueryTestValidator.validateEQ(new APIQueryQuotaMsg(), api, APIQueryQuotaReply.class, quota);
        QueryTestValidator.validateRandomEQConjunction(new APIQueryQuotaMsg(), api, APIQueryQuotaReply.class, quota, 3);

        api.deleteAccount(test.getUuid(), identityCreator.getAccountSession());
        SimpleQuery<QuotaVO> qq = dbf.createQuery(QuotaVO.class);
        qq.add(QuotaVO_.identityUuid, Op.EQ, test.getUuid());
        Assert.assertFalse(qq.isExists());
        Assert.assertFalse(dbf.isExist(test.getUuid(), AccountVO.class));
    }
}

