package org.zstack.test.core.scheduler;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.scheduler.SchedulerConstant;
import org.zstack.header.core.scheduler.SchedulerVO;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.QuotaInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.identity.IdentityCreator;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by root on 8/23/16.
 */
public class TestSchedulerQuota {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    @Rule
    public ExpectedException thrown = ExpectedException.none();

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
        VmInstanceInventory inv = api.listVmInstances(null).get(0);
        IdentityCreator identityCreator = new IdentityCreator(api);
        api.createAccount("Test", "Test");
        AccountInventory test = identityCreator.useAccount("test");
        api.shareResource(api.listL3Network(null).stream().map(L3NetworkInventory::getUuid).collect(Collectors.toList()),
                Arrays.<String>asList(test.getUuid()),
                true);
        api.changeResourceOwner(inv.getUuid(), test.getUuid());

        api.updateQuota(test.getUuid(), SchedulerConstant.QUOTA_SCHEDULER_NUM, 0);
        QuotaInventory schedulerQuotaInv = api.getQuota(SchedulerConstant.QUOTA_SCHEDULER_NUM, test.getUuid(), null);
        Assert.assertEquals(0, schedulerQuotaInv.getValue());

        thrown.expect(ApiSenderException.class);
        thrown.expectMessage(SchedulerConstant.QUOTA_SCHEDULER_NUM);
        {

            // create start vm scheduler, will not take effect at start due to vm status is running
            Date date = new Date();
            String type = "simple";
            Long startDate = date.getTime() / 1000;
            Integer interval = 5;
            String uuid = inv.getUuid();
            Integer repeatCount = 10;
            api.startVmInstanceScheduler(uuid, type, startDate, interval, repeatCount, identityCreator.getAccountSession());
        }
        // check scheduler num
        List<SchedulerVO> vos = dbf.listAll(SchedulerVO.class);
        Assert.assertEquals(0, vos.size());
    }
}
