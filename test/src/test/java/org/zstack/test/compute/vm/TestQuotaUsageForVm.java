package org.zstack.test.compute.vm;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.Quota;
import org.zstack.header.identity.QuotaInventory;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.VmCreator;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.identity.IdentityCreator;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import java.util.List;

/**
 * test vm quota usage
 */
public class TestQuotaUsageForVm {
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
        DiskOfferingInventory disk = deployer.diskOfferings.get("disk50G");

        IdentityCreator identityCreator = new IdentityCreator(api);
        AccountInventory test = identityCreator.useAccount("test");

        VmCreator vmCreator = new VmCreator(api);
        vmCreator.imageUuid = img.getUuid();
        vmCreator.addL3Network(l3.getUuid());
        vmCreator.instanceOfferingUuid = ioinv.getUuid();
        vmCreator.name = "vm";
        vmCreator.addDisk(disk.getUuid());
        vmCreator.addDisk(disk.getUuid());
        vmCreator.session = identityCreator.getAccountSession();
        VmInstanceInventory vm = vmCreator.create();

        List<Quota.QuotaUsage> usages = api.getQuotaUsage(test.getUuid(), null);
        {
            Quota.QuotaUsage totalVmNum = CollectionUtils.find(usages, new Function<Quota.QuotaUsage, Quota.QuotaUsage>() {
                @Override
                public Quota.QuotaUsage call(Quota.QuotaUsage arg) {
                    return VmInstanceConstant.QUOTA_VM_TOTAL_NUM.equals(arg.getName()) ? arg : null;
                }
            });
            Assert.assertNotNull(totalVmNum);
        }

        {
            Quota.QuotaUsage vmNum = CollectionUtils.find(usages, new Function<Quota.QuotaUsage, Quota.QuotaUsage>() {
                @Override
                public Quota.QuotaUsage call(Quota.QuotaUsage arg) {
                    return VmInstanceConstant.QUOTA_VM_RUNNING_NUM.equals(arg.getName()) ? arg : null;
                }
            });
            Assert.assertNotNull(vmNum);

            QuotaInventory qvm = api.getQuota(VmInstanceConstant.QUOTA_VM_RUNNING_NUM, test.getUuid(), identityCreator.getAccountSession());
            Assert.assertEquals(qvm.getValue(), vmNum.getTotal().longValue());
            Assert.assertEquals(1, vmNum.getUsed().longValue());
        }

        {
            Quota.QuotaUsage cpuNum = CollectionUtils.find(usages, new Function<Quota.QuotaUsage, Quota.QuotaUsage>() {
                @Override
                public Quota.QuotaUsage call(Quota.QuotaUsage arg) {
                    return VmInstanceConstant.QUOTA_VM_RUNNING_CPU_NUM.equals(arg.getName()) ? arg : null;
                }
            });
            Assert.assertNotNull(cpuNum);
            QuotaInventory qvm = api.getQuota(VmInstanceConstant.QUOTA_VM_RUNNING_CPU_NUM, test.getUuid(), identityCreator.getAccountSession());
            Assert.assertEquals(qvm.getValue(), cpuNum.getTotal().longValue());
            Assert.assertEquals(vm.getCpuNum().intValue(), cpuNum.getUsed().intValue());
        }

        {
            Quota.QuotaUsage mem = CollectionUtils.find(usages, new Function<Quota.QuotaUsage, Quota.QuotaUsage>() {
                @Override
                public Quota.QuotaUsage call(Quota.QuotaUsage arg) {
                    return VmInstanceConstant.QUOTA_VM_RUNNING_MEMORY_SIZE.equals(arg.getName()) ? arg : null;
                }
            });
            Assert.assertNotNull(mem);
            QuotaInventory qvm = api.getQuota(VmInstanceConstant.QUOTA_VM_RUNNING_MEMORY_SIZE, test.getUuid(), identityCreator.getAccountSession());
            Assert.assertEquals(qvm.getValue(), mem.getTotal().longValue());
            Assert.assertEquals(vm.getMemorySize(), mem.getUsed());
        }

        {
            Quota.QuotaUsage volnum = CollectionUtils.find(usages, new Function<Quota.QuotaUsage, Quota.QuotaUsage>() {
                @Override
                public Quota.QuotaUsage call(Quota.QuotaUsage arg) {
                    return VolumeConstant.QUOTA_DATA_VOLUME_NUM.equals(arg.getName()) ? arg : null;
                }
            });
            Assert.assertNotNull(volnum);
            QuotaInventory qvm = api.getQuota(VolumeConstant.QUOTA_DATA_VOLUME_NUM, test.getUuid(), identityCreator.getAccountSession());
            Assert.assertEquals(qvm.getValue(), volnum.getTotal().longValue());
            Assert.assertEquals(vm.getAllVolumes().size() - 1, volnum.getUsed().intValue());
        }

        {
            Quota.QuotaUsage volsize = CollectionUtils.find(usages, new Function<Quota.QuotaUsage, Quota.QuotaUsage>() {
                @Override
                public Quota.QuotaUsage call(Quota.QuotaUsage arg) {
                    return VolumeConstant.QUOTA_VOLUME_SIZE.equals(arg.getName()) ? arg : null;
                }
            });
            Assert.assertNotNull(volsize);
            QuotaInventory qvm = api.getQuota(VolumeConstant.QUOTA_VOLUME_SIZE, test.getUuid(), identityCreator.getAccountSession());
            Assert.assertEquals(qvm.getValue(), volsize.getTotal().longValue());

            long size = 0;
            for (VolumeInventory v : vm.getAllVolumes()) {
                size += v.getSize();
            }
            Assert.assertEquals(size, volsize.getUsed().longValue());
        }
    }
}

