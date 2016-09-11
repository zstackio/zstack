package org.zstack.test.mevoco.alert;

import junit.framework.Assert;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.zstack.alert.*;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.prometheus.KvmVmAlarmFactory;
import org.zstack.prometheus.PrometheusAlertRuleBuilder;
import org.zstack.prometheus.PrometheusConstant;
import org.zstack.prometheus.PrometheusManager.AlertID;
import org.zstack.simulator.storage.backup.sftp.SftpBackupStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.zstack.prometheus.PrometheusManager.makeExpression;

public class TestKvmVmAlarm {
    CLogger logger = Utils.getLogger(TestKvmVmAlarm.class);
    Deployer deployer;
    Api api;
    ComponentLoader loader;
    CloudBus bus;
    DatabaseFacade dbf;
    SessionInventory session;
    SftpBackupStorageSimulatorConfig config;
    KvmVmAlarmFactory alarmFactory;

    @Before
    public void setUp() throws Exception {
        File ruleFile = getRuleFile();
        if (ruleFile.exists()) {
            ruleFile.delete();
        }

        DBUtil.reDeployDB();
        WebBeanConstructor con = new WebBeanConstructor();
        deployer = new Deployer("deployerXml/kvm/TestCreateVmOnKvm.xml", con);
        deployer.addSpringConfig("KVMRelated.xml");
        deployer.addSpringConfig("prometheus.xml");
        deployer.addSpringConfig("alert.xml");
        deployer.build();
        api = deployer.getApi();
        loader = deployer.getComponentLoader();
        bus = loader.getComponent(CloudBus.class);
        dbf = loader.getComponent(DatabaseFacade.class);
        config = loader.getComponent(SftpBackupStorageSimulatorConfig.class);
        alarmFactory = loader.getComponent(KvmVmAlarmFactory.class);
        session = api.loginAsAdmin();
    }

    private File getRuleFile() {
        return new File(PathUtil.join(PrometheusConstant.ALERT_RULES_ROOT, String.format("%s.yml", AlarmConstant.ALERT_CATEGORY_VM_CPU)));
    }

	@Test
	public void test() throws InterruptedException, ApiSenderException, IOException {
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
        Assert.assertTrue(dbf.isExist(inv.getUuid(), AlarmVO.class));

        AlertID id = new AlertID(AlarmConstant.ALERT_CATEGORY_VM_CPU, vm.getUuid());

        PrometheusAlertRuleBuilder rb = new PrometheusAlertRuleBuilder();
        rb.setName(id.toString());
        rb.setDuration(inv.getConditionDuration());
        rb.setLabels(inv.getLabels());
        rb.setExpression(makeExpression(PrometheusConstant.VM_CPU_CONDITION_NAME, inv.getConditionOperator(), inv.getConditionValue()));
        String rule1 = rb.toString();

        logger.debug(String.format("rule: %s", rule1));
        File cpuRuleFile = getRuleFile();
        String cpuRule = FileUtils.readFileToString(cpuRuleFile);
        logger.debug(String.format("rule in file: %s", cpuRule));
        Assert.assertTrue(cpuRule.contains(rule1));

        msg = new APICreateVmCpuAlarmMsg();
        msg.setName("test2");
        msg.setVmInstanceUuid(vm.getUuid());
        msg.setConditionDuration(2);
        msg.setConditionName(VmAlarmFactory.CPU_ALARM);
        msg.setConditionOperator(AlarmConditionOp.GT);
        msg.setConditionValue("2");
        evt = api.sendApiMessage(msg, APICreateAlarmEvent.class);
        AlarmInventory inv2 = evt.getInventory();
        Assert.assertTrue(dbf.isExist(inv2.getUuid(), AlarmVO.class));

        rb = new PrometheusAlertRuleBuilder();
        rb.setName(id.toString());
        rb.setDuration(inv2.getConditionDuration());
        rb.setLabels(inv2.getLabels());
        rb.setExpression(makeExpression(PrometheusConstant.VM_CPU_CONDITION_NAME, inv.getConditionOperator(), inv2.getConditionValue()));
        String rule2 = rb.toString();

        logger.debug(String.format("rule: %s", rule2));
        cpuRuleFile = getRuleFile();
        cpuRule = FileUtils.readFileToString(cpuRuleFile);
        logger.debug(String.format("rule in file: %s", cpuRule));
        Assert.assertTrue(cpuRule.contains(rule2));

        // delete the rule file and use alarmFactory to re-create
        cpuRuleFile.delete();
        alarmFactory.managementNodeReady();
        TimeUnit.SECONDS.sleep(2);
        cpuRule = FileUtils.readFileToString(cpuRuleFile);
        Assert.assertTrue(cpuRule.contains(rule1));
        Assert.assertTrue(cpuRule.contains(rule2));

        api.deleteAlarm(inv2.getUuid(), null);
        cpuRule = FileUtils.readFileToString(cpuRuleFile);
        Assert.assertFalse(cpuRule.contains(rule2));

        api.deleteAlarm(inv.getUuid(), null);
        cpuRule = FileUtils.readFileToString(cpuRuleFile);
        cpuRule = cpuRule.replaceAll("\n", "").replaceAll(" ", "");
        Assert.assertTrue(cpuRule.isEmpty());
    }
}
