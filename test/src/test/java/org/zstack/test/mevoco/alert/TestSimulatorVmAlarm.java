package org.zstack.test.mevoco.alert;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zstack.alert.*;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.deployer.Deployer;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public class TestSimulatorVmAlarm {
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SimulatorAlarmFactory factory;
    AlertManager mgr;
    EventFacade evtf;
    volatile int count;

    @Before
    public void setUp() throws Exception {
        DBUtil.reDeployDB();
        deployer = new Deployer("deployerXml/vm/TestCreateVm.xml");
        deployer.addSpringConfig("alarmSimulator.xml");
        deployer.addSpringConfig("alert.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        factory = loader.getComponent(SimulatorAlarmFactory.class);
        mgr = loader.getComponent(AlertManager.class);
        evtf = loader.getComponent(EventFacade.class);
    }

    @Test
    public void test() throws ApiSenderException, InterruptedException {
        evtf.on(AlertCanonicalEvents.FIRE_ALERT_PATH, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                count++;
            }
        });

        VmInstanceInventory vm = deployer.vms.get("TestVm");

        APICreateVmCpuAlarmMsg msg = new APICreateVmCpuAlarmMsg();
        msg.setName("test");
        msg.setVmInstanceUuid(vm.getUuid());
        msg.setConditionDuration(1);
        msg.setConditionName(VmAlarmFactory.CPU_ALARM);
        msg.setConditionOperator(AlarmConditionOp.GT);
        msg.setConditionValue("1");
        APICreateAlarmEvent evt = api.sendApiMessage(msg, APICreateAlarmEvent.class);

        AlarmInventory inv = evt.getInventory();
        Assert.assertEquals(1, factory.alarms.size());
        Assert.assertEquals(vm.getUuid(), inv.getLabel(VmAlarmFactory.LABEL_VM_UUID));
        Assert.assertEquals(msg.getConditionName(), inv.getConditionName());
        Assert.assertEquals(msg.getConditionOperator().getName(), inv.getConditionOperator());
        Assert.assertEquals(msg.getConditionDuration(), inv.getConditionDuration().longValue());
        Assert.assertEquals(msg.getConditionValue(), inv.getConditionValue());

        // test alert
        Map ids = map(e(VmAlarmFactory.LABEL_VM_UUID, vm.getUuid()));
        AlertSender sender = new AlertSender();
        sender.setName("test");
        sender.setDescription("test");
        sender.setIds(ids);
        sender.setLabels(map(e("test", "test")));
        sender.send();
        TimeUnit.SECONDS.sleep(2);

        String id = AlertSender.labelsToAlertId(ids);
        AlertVO avo = dbf.findByUuid(id, AlertVO.class);
        AlertInventory ainv = AlertInventory.valueOf(avo);
        Assert.assertEquals(2, ainv.getLabels().size());
        Assert.assertEquals(vm.getUuid(), ainv.getLabels().get(VmAlarmFactory.LABEL_VM_UUID));
        Assert.assertEquals("test", ainv.getLabels().get("test"));
        Assert.assertEquals(AlertStatus.Active, avo.getStatus());
        Assert.assertEquals(1, ainv.getCount());
        Assert.assertEquals(1, ainv.getTimestamps().size());
        Assert.assertEquals(1, count);

        // alert is muted
        AlertUpdater updater = new AlertUpdater(api);
        updater.status = AlertStatus.Muted;
        updater.update(avo.getUuid());
        avo = dbf.reload(avo);
        Assert.assertEquals(AlertStatus.Muted, avo.getStatus());

        sender.send();
        TimeUnit.SECONDS.sleep(2);
        avo = dbf.reload(avo);
        Assert.assertEquals(2, avo.getCount());
        Assert.assertEquals(1, count);

        // alert is enabled again
        updater.status = AlertStatus.Active;
        updater.update(avo.getUuid());
        avo = dbf.reload(avo);
        Assert.assertEquals(AlertStatus.Active, avo.getStatus());

        sender.send();
        TimeUnit.SECONDS.sleep(2);
        avo = dbf.reload(avo);
        Assert.assertEquals(3, avo.getCount());
        Assert.assertEquals(2, count);
    }
}
