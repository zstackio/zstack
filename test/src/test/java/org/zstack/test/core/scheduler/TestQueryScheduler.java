package org.zstack.test.core.scheduler;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.scheduler.*;
import org.zstack.header.core.scheduler.SchedulerInventory;
import org.zstack.header.core.scheduler.SchedulerVO;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.query.QueryCondition;
import org.zstack.simulator.kvm.VolumeSnapshotKvmSimulator;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.test.search.QueryTestValidator;

import java.util.ArrayList;

/**
 * Created by root on 7/20/16.
 */
public class TestQueryScheduler {
    ComponentLoader loader;
    Api api;
    @Autowired
    SchedulerFacade scheduler;
    DatabaseFacade dbf;
    CloudBus bus;
    Deployer deployer;
    SessionInventory session;
    VolumeSnapshotKvmSimulator snapshotKvmSimulator;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestCreateVmOnKvm.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("SchedulerFacade.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        dbf = loader.getComponent(DatabaseFacade.class);
        bus = loader.getComponent(CloudBus.class);
        scheduler = loader.getComponent(SchedulerFacade.class);
        snapshotKvmSimulator = loader.getComponent(VolumeSnapshotKvmSimulator.class);
        session = api.loginAsAdmin();
    }

    @Test
    public void test() throws InterruptedException, ApiSenderException, SchedulerException {
        SchedulerVO vo = new SchedulerVO();
        vo.setSchedulerName("test-query");
        vo.setSchedulerType("cron");
        vo.setCronScheduler("1 * * * * ?");
        vo.setUuid(Platform.getUuid());
        vo = dbf.persistAndRefresh(vo);
        SchedulerInventory inv = SchedulerInventory.valueOf(vo);
        APIQuerySchedulerMsg msg = new APIQuerySchedulerMsg();
        QueryTestValidator.validateEQ(msg, api, APIQuerySchedulerReply.class, inv);
        msg.setConditions(new ArrayList<QueryCondition>());
        APIQuerySchedulerReply reply = api.query(msg, APIQuerySchedulerReply.class);
        Assert.assertEquals(1, reply.getInventories().size());

    }

}
