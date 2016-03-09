package org.zstack.test.cascade;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.host.HostVO;
import org.zstack.header.image.ImageVO;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l3.*;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.zone.ZoneVO;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

/**
 *
 * 1. delete ip range
 * 2. add a new ip range
 * 3. start vm
 *
 * confirm the nic on deleted IP range gets a new IP, but other nics keep old IPs
 */
public class TestCascadeDeletion35 {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestCreateVm.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        SimpleQuery<IpRangeVO> q = dbf.createQuery(IpRangeVO.class);
        q.add(IpRangeVO_.name, SimpleQuery.Op.EQ, "TestIpRange1");
        final IpRangeVO ipr = q.find();
        VmInstanceInventory vm = deployer.vms.get("TestVm");
        int nicNum = vm.getVmNics().size();
        VmNicInventory nic = CollectionUtils.find(vm.getVmNics(), new Function<VmNicInventory, VmNicInventory>() {
            @Override
            public VmNicInventory call(VmNicInventory arg) {
                UsedIpVO ip = dbf.findByUuid(arg.getUsedIpUuid(), UsedIpVO.class);
                return ip.getIpRangeUuid().equals(ipr.getUuid()) ? arg : null;
            }
        });

        api.deleteIpRange(ipr.getUuid());

        IpRangeInventory newIpr = api.addIpRangeByCidr(ipr.getL3NetworkUuid(), "172.16.2.0/24");

        VmInstanceInventory vm1 = api.startVmInstance(vm.getUuid());

        Assert.assertEquals(nicNum, vm1.getVmNics().size());
        for (VmNicInventory n : vm1.getVmNics()) {
            if (n.getIp().equals(nic.getIp())) {
                Assert.fail(String.format("VM still get an old IP(%s)", nic.getIp()));
            }
        }

        b1:
        for (VmNicInventory n : vm.getVmNics()) {
            if (n.getUuid().equals(nic.getUuid())) {
                continue;
            }

            for (VmNicInventory n1 : vm1.getVmNics()) {
                if (n1.getIp().equals(n.getIp())) {
                    break b1;
                }
            }

            Assert.fail(String.format("nic[uuid:%s] incorrectly lost its old IP[%s]", n.getUuid(), n.getIp()));
        }
    }
}
