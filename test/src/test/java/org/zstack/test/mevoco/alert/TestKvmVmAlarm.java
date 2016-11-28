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
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.prometheus.AlertRule;
import org.zstack.prometheus.KvmVmAlarmFactory;
import org.zstack.prometheus.PrometheusAlertController.AlertInternal;
import org.zstack.prometheus.PrometheusConstant;
import org.zstack.simulator.storage.backup.sftp.SftpBackupStorageSimulatorConfig;
import org.zstack.test.Api;
import org.zstack.test.ApiSenderException;
import org.zstack.test.DBUtil;
import org.zstack.test.WebBeanConstructor;
import org.zstack.test.deployer.Deployer;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;

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
    RESTFacade restf;

    @Before
    public void setUp() throws Exception {
        File ruleFile = getCpuRuleFile();
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
        restf = loader.getComponent(RESTFacade.class);
        session = api.loginAsAdmin();
    }

    private File getCpuRuleFile() {
        return new File(PrometheusConstant.VM_CPU_ALERT_FILE_PATH);
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

        AlertRule alertRule1 = AlertRule.createRule(inv, vm.getUuid());
        String rule1 = alertRule1.generateRuleText();

        logger.debug(String.format("rule: %s", rule1));
        File cpuRuleFile = getCpuRuleFile();
        String cpuRule = FileUtils.readFileToString(cpuRuleFile);
        logger.debug(String.format("rule in file: %s", cpuRule));
        Assert.assertTrue(cpuRule.contains(rule1));

        testAlert(vm.getUuid(), inv);

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

        AlertRule alertRule2 = AlertRule.createRule(inv2, vm.getUuid());
        String rule2 = alertRule2.generateRuleText();

        logger.debug(String.format("rule: %s", rule2));
        cpuRuleFile = getCpuRuleFile();
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

    private void sendAlert(String vmUuid, AlarmInventory inv) {
        AlertRule rule = AlertRule.createRule(inv, vmUuid);

        AlertInternal alert = new AlertInternal();
        alert.labels = new HashMap<>();
        alert.annotations = new HashMap<>();
        alert.annotations.put(PrometheusConstant.ANNOTATION_ALARM_UUID, inv.getUuid());
        alert.annotations.put(PrometheusConstant.ANNOTATION_ALERT_TYPE, inv.getConditionName());
        alert.annotations.put(PrometheusConstant.ANNOTATION_RESOURCE_UUID, vmUuid);
        alert.annotations.put(PrometheusConstant.ANNOTATION_ALERT_ID, rule.getId());

        String url = restf.makeUrl(PrometheusConstant.ALERT_URL);
        restf.getRESTTemplate().postForEntity(URI.create(url), JSONObjectUtil.toJsonString(asList(alert)), String.class);
    }

    private void testAlert(String vmUuid, AlarmInventory inv) {
        sendAlert(vmUuid, inv);

        List<AlertVO> vos = dbf.listAll(AlertVO.class);
        AlertVO vo = vos.stream().filter((it) -> {
            int count = 0;
            for (AlertLabelVO lvo : it.getLabels()) {
                if (lvo.getValue().equals(vmUuid) || lvo.getValue().equals(VmAlarmFactory.CPU_ALARM)) {
                    count++;
                }

                if (count == 2) {
                    return true;
                }
            }

            return false;
        }).findAny().get();
        Assert.assertNotNull(vo);

        String name = null;
        String description = null;
        if (VmAlarmFactory.CPU_ALARM.equals(inv.getConditionName())) {
            if (AlarmConditionOp.GT.getName().equals(inv.getConditionOperator())) {
                name = AlertI18n.VM_CPU_ALERT_GT_NAME;
                description = AlertI18n.VM_CPU_ALERT_GT_DESCRIPTION;
            } else if (AlarmConditionOp.LT.getName().equals(inv.getConditionOperator())) {
                name = AlertI18n.VM_CPU_ALERT_LT_NAME;
                description = AlertI18n.VM_CPU_ALERT_LT_DESCRIPTION;
            } else if (AlarmConditionOp.EQ.getName().equals(inv.getConditionOperator())) {
                name = AlertI18n.VM_CPU_ALERT_EQ_NAME;
                description = AlertI18n.VM_CPU_ALERT_EQ_DESCRIPTION;
            } else if (AlarmConditionOp.NOT_EQ.getName().equals(inv.getConditionOperator())) {
                name = AlertI18n.VM_CPU_ALERT_NOT_EQ_NAME;
                description = AlertI18n.VM_CPU_ALERT_NOT_EQ_DESCRIPTION;
            }
        } else if (VmAlarmFactory.MEM_ALARM.equals(inv.getConditionName())) {
            if (AlarmConditionOp.GT.getName().equals(inv.getConditionOperator())) {
                name = AlertI18n.VM_MEM_ALERT_GT_NAME;
                description = AlertI18n.VM_MEM_ALERT_GT_DESCRIPTION;
            } else if (AlarmConditionOp.LT.getName().equals(inv.getConditionOperator())) {
                name = AlertI18n.VM_MEM_ALERT_LT_NAME;
                description = AlertI18n.VM_MEM_ALERT_LT_DESCRIPTION;
            } else if (AlarmConditionOp.EQ.getName().equals(inv.getConditionOperator())) {
                name = AlertI18n.VM_MEM_ALERT_EQ_NAME;
                description = AlertI18n.VM_MEM_ALERT_EQ_DESCRIPTION;
            } else if (AlarmConditionOp.NOT_EQ.getName().equals(inv.getConditionOperator())) {
                name = AlertI18n.VM_MEM_ALERT_NOT_EQ_NAME;
                description = AlertI18n.VM_MEM_ALERT_NOT_EQ_DESCRIPTION;
            }
        }

        Assert.assertEquals(name, vo.getName());
        Assert.assertEquals(description, vo.getDescription());
        Assert.assertNotNull(AlertInventory.valueOf(vo).getLabel(AlarmConstant.ALERT_I18N_PARAMS));

        // send again, confirm only one alert in database but count is 2
        sendAlert(vmUuid, inv);
        vos = dbf.listAll(AlertVO.class);
        Assert.assertEquals(1, vos.size());
        vo = vos.get(0);
        Assert.assertEquals(2, vo.getCount());
    }
}
